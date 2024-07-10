package com.souzalima.fluxodeencomendas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private Button searchButton; // Alteração do SearchView para Button
    private OrderAdapter adapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        // Inicialização dos componentes da interface
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        searchButton = findViewById(R.id.searchButton); // Referenciando o botão

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
    }

    // Método para carregar as últimas encomendas do Firebase
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

    // Método getter para acessar a lista de encomendas
    public List<Order> getOrderList() {
        return orderList;
    }
}
