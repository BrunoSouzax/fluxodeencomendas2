package com.souzalima.fluxodeencomendas;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private Button searchButton; // Botão para iniciar a pesquisa
    private Button exportButton; // Botão para exportar relatório
    private OrderAdapter adapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        // Inicialização dos componentes da interface
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        searchButton = findViewById(R.id.searchButton); // Referenciando o botão de pesquisa
        exportButton = findViewById(R.id.buttonExport); // Referenciando o botão de exportação

        orderList = new ArrayList<>();
        adapter = new OrderAdapter(this, orderList);

        // Configuração do RecyclerView
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(adapter);

        // Carregar encomendas do Firebase
        loadOrdersFromFirebase();

        // Configurar o clique do botão para iniciar a pesquisa
        searchButton.setOnClickListener(v -> {
            // Iniciar a atividade de pesquisa
            Intent intent = new Intent(OrdersActivity.this, SearchResultsActivity.class);
            startActivity(intent);
        });

        // Configurar o clique do botão para exportar o relatório
        exportButton.setOnClickListener(v -> {
            exportToExcel(orderList);
        });
    }

    /**
     * Método para carregar as últimas encomendas do Firebase
     */
    private void loadOrdersFromFirebase() {
        Query query = FirebaseDatabase.getInstance().getReference().child("orders")
                .orderByChild("timestamp") // Supondo que você tenha um campo de timestamp em seus dados
                .limitToLast(50); // Ajuste conforme necessário

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null) {
                        orderList.add(0, order); // Adicionando na posição 0 para exibir mais recentes primeiro
                    }
                }
                adapter.notifyDataSetChanged(); // Notifica o RecyclerView sobre as mudanças na lista
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OrdersActivity.this, "Erro ao carregar encomendas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Método para exportar a lista de encomendas para um arquivo Excel
     * @param orders Lista de encomendas a serem exportadas
     */
    private void exportToExcel(List<Order> orders) {
        // Nome do arquivo
        String fileName = "Encomendas_" + System.currentTimeMillis() + ".xlsx";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Encomendas");

        // Cabeçalho
        String[] headers = {"Unidade", "Nome", "Data", "Status", "Descrição"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Dados
        int rowNum = 1;
        for (Order order : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(order.getUnit());
            row.createCell(1).setCellValue(order.getName());
            row.createCell(2).setCellValue(order.getDate());
            row.createCell(3).setCellValue(order.getStatus());
            row.createCell(4).setCellValue(order.getDescription());
        }

        // Escrever o workbook para o arquivo
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
            Toast.makeText(this, "Relatório exportado: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Após exportar, oferecer opção de compartilhar via WhatsApp
            shareExcelFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao exportar relatório", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Método para compartilhar o arquivo Excel via WhatsApp
     * @param file Arquivo a ser compartilhado
     */
    private void shareExcelFile(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        Uri uri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", file);

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Encomendas");
        intent.putExtra(Intent.EXTRA_TEXT, "Segue o relatório de encomendas");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Compartilhar via"));
    }


    /**
     * Método getter para acessar a lista de encomendas
     * @return Lista de encomendas
     */
    public List<Order> getOrderList() {
        return orderList;
    }
}
