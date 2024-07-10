package com.souzalima.fluxodeencomendas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView textViewUnit;
    private TextView textViewName;
    private TextView textViewDate;
    private TextView textViewTime;
    private TextView textViewDescription;
    private TextView textViewNF;
    private TextView textViewPlantonista;
    private TextView textViewStatus;
    private EditText editTextRecebedor;
    private EditText editTextHorarioEntrega;
    private Button buttonRequisicao;
    private ImageView imageViewPhoto;

    private static final String PREFS_NAME = "OrderDetailsPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Inicialização dos componentes visuais
        textViewUnit = findViewById(R.id.textViewUnit);
        textViewName = findViewById(R.id.textViewName);
        textViewDate = findViewById(R.id.textViewDate);
        textViewTime = findViewById(R.id.textViewTime);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewNF = findViewById(R.id.textViewNF);
        textViewPlantonista = findViewById(R.id.textViewPlantonista);
        textViewStatus = findViewById(R.id.textViewStatus);
        editTextRecebedor = findViewById(R.id.editTextRecebedor);
        editTextHorarioEntrega = findViewById(R.id.editTextHorarioEntrega);
        buttonRequisicao = findViewById(R.id.buttonRequisicao);
        imageViewPhoto = findViewById(R.id.imageViewPhoto);

        // Obtenção dos dados da encomenda passados pela Intent
        final Order order = (Order) getIntent().getSerializableExtra("ORDER");

        if (order != null) {
            // Preenchimento dos campos com os dados da encomenda
            textViewUnit.setText("Unidade do Apartamento: " + order.getUnit());
            textViewName.setText("Nome do Destinatário: " + order.getName());
            textViewDate.setText("Data: " + order.getDate());
            textViewTime.setText("Hora: " + order.getTime());
            textViewDescription.setText("Descrição: " + order.getDescription());
            textViewNF.setText("Nota Fiscal: " + (TextUtils.isEmpty(order.getNotaFiscal()) ? "-" : order.getNotaFiscal()));
            textViewPlantonista.setText("Plantonista: " + order.getPlantonista());

            // Carregamento da imagem usando Glide, se houver URL válida
            if (order.getImageUrl() != null && !order.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(order.getImageUrl())
                        .into(imageViewPhoto);
            }

            // Configuração do clique do botão de Requisição
            buttonRequisicao.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Salvar os detalhes da encomenda localmente
                    saveOrderDetails(order);
                    // Atualizar o status da encomenda no banco de dados
                    updateOrderStatus(order);
                }
            });

            // Definição do status com base no estado da encomenda
            if (order.getStatus() != null && !order.getStatus().isEmpty()) {
                switch (order.getStatus().toLowerCase()) {
                    case "postado":
                        textViewStatus.setText("Status: Postado");
                        break;
                    case "pendente":
                        textViewStatus.setText("Status: Pendente");
                        break;
                    case "entregue":
                        // Verificação se os campos de recebedor e horário de entrega estão preenchidos
                        String recebedor = editTextRecebedor.getText().toString();
                        String horarioEntrega = editTextHorarioEntrega.getText().toString();
                        if (!TextUtils.isEmpty(recebedor) && !TextUtils.isEmpty(horarioEntrega)) {
                            textViewStatus.setText("Status: Entregue por " + recebedor + " às " + horarioEntrega);
                        } else {
                            textViewStatus.setText("Status: Entregue");
                        }
                        break;
                    default:
                        textViewStatus.setText("Status: Desconhecido");
                        break;
                }
            }

            // Configuração do clique na imagem para abrir em tela cheia
            imageViewPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Iniciar a Activity de tela cheia com a imagem ampliada
                    Intent intent = new Intent(OrderDetailActivity.this, PhotoFullscreenActivity.class);
                    intent.putExtra("IMAGE_URL", order.getImageUrl()); // Envie a URL da imagem para a tela cheia
                    startActivity(intent);
                }
            });
        }
    }

    // Método para salvar os detalhes da encomenda localmente
    private void saveOrderDetails(Order order) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("unit", order.getUnit());
        editor.putString("name", order.getName());
        editor.putString("date", order.getDate());
        editor.putString("time", order.getTime());
        editor.putString("description", order.getDescription());
        editor.putString("notaFiscal", order.getNotaFiscal());
        editor.putString("plantonista", order.getPlantonista());
        editor.putString("status", order.getStatus());
        editor.apply();
    }

    // Método para atualizar o status da encomenda no banco de dados Firebase
    private void updateOrderStatus(Order order) {
        if (order.getOrderId() != null) {
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(order.getOrderId());
            orderRef.child("status").setValue("requisitada")
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Exibir mensagem de sucesso ao requisitar a encomenda
                            Toast.makeText(OrderDetailActivity.this, "Encomenda requisitada com sucesso", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Exibir mensagem de erro em caso de falha ao requisitar a encomenda
                            Toast.makeText(OrderDetailActivity.this, "Falha ao requisitar encomenda: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Log de erro caso o ID da encomenda seja nulo
            Log.e("OrderDetailActivity", "Erro: ID da encomenda é nulo");
            Toast.makeText(OrderDetailActivity.this, "Erro: ID da encomenda é nulo", Toast.LENGTH_SHORT).show();
        }
    }
}
