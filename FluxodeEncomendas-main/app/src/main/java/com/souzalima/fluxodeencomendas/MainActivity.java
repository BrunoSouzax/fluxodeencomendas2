package com.souzalima.fluxodeencomendas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private DatabaseReference database;
    private EditText editTextUnit, editTextName, editTextDate, editTextTime, editTextDescription, editTextNotaFiscal;
    private Spinner spinnerPlantonista;
    private EditText editTextOutroPlantonista;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar e solicitar permissões necessárias
        checkPermissions();

        // Inicializar elementos da interface do usuário
        editTextUnit = findViewById(R.id.editTextUnit);
        editTextName = findViewById(R.id.editTextName);
        editTextDate = findViewById(R.id.editTextDate);
        editTextTime = findViewById(R.id.editTextTime);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextNotaFiscal = findViewById(R.id.editTextNF); // Novo campo adicionado
        spinnerPlantonista = findViewById(R.id.spinnerPlantonista);
        editTextOutroPlantonista = findViewById(R.id.editTextOutroPlantonista);
        Button buttonTakePhoto = findViewById(R.id.buttonTakePhoto);
        Button buttonAddOrder = findViewById(R.id.buttonAddOrder);
        Button buttonClear = findViewById(R.id.buttonClear);

        buttonClear.setOnClickListener(v -> clearFields());

        // Inicializar referência do Firebase
        database = FirebaseDatabase.getInstance().getReference();

        // Configurar o Spinner de Plantonistas
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.plantonista_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlantonista.setAdapter(adapter);
        spinnerPlantonista.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                if (selectedOption.equals("Outro")) {
                    editTextOutroPlantonista.setVisibility(View.VISIBLE);
                } else {
                    editTextOutroPlantonista.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Não é necessário fazer nada aqui
            }
        });

        // Configurar o clique do botão para tirar uma foto
        buttonTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());

        // Configurar o clique do botão para adicionar uma nova encomenda
        buttonAddOrder.setOnClickListener(v -> {
            String unit = editTextUnit.getText().toString();
            String name = editTextName.getText().toString();
            String date = editTextDate.getText().toString();
            String time = editTextTime.getText().toString();
            String description = editTextDescription.getText().toString();
            String notaFiscal = editTextNotaFiscal.getText().toString(); // Capturar valor da nota fiscal
            String plantonista;
            if (editTextOutroPlantonista.getVisibility() == View.VISIBLE) {
                plantonista = editTextOutroPlantonista.getText().toString();
            } else {
                plantonista = spinnerPlantonista.getSelectedItem().toString();
            }

            // Verificar se todos os campos foram preenchidos
            if (!unit.isEmpty() && !name.isEmpty() && !date.isEmpty() && !time.isEmpty() && !description.isEmpty() && !notaFiscal.isEmpty() && !plantonista.isEmpty()) {
                String orderId = database.child("orders").push().getKey(); // Gera um novo ID único
                Order order = new Order();
                order.setUnit(unit);
                order.setName(name);
                order.setDate(date);
                order.setTime(time);
                order.setDescription(description);
                order.setNotaFiscal(notaFiscal); // Definir valor da nota fiscal
                order.setPlantonista(plantonista);
                order.setImageUrl(currentPhotoPath);
                order.setOrderId(orderId); // Define o ID da encomenda
                order.setStatus("-");

                // Fazer o upload da imagem para o Firebase Storage
                if (currentPhotoPath != null) {
                    uploadImageToFirebaseStorage(currentPhotoPath, order, orderId);
                } else {
                    database.child("orders").child(orderId).setValue(order);
                    Toast.makeText(MainActivity.this, "Encomenda adicionada com sucesso", Toast.LENGTH_SHORT).show();
                }

                // Enviar mensagem pelo WhatsApp
                sendWhatsAppMessage(unit, name, date, time, description, notaFiscal, plantonista, currentPhotoPath);
            } else {
                Toast.makeText(MainActivity.this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
            }
        });

        // Preencher data e hora atuais automaticamente
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        String currentTime = timeFormat.format(new Date());
        editTextDate.setText(currentDate);
        editTextTime.setText(currentTime);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            // Lógica para a opção Adicionar aqui
            return true;
        } else if (id == R.id.action_consultar) {
            startActivity(new Intent(MainActivity.this, OrdersActivity.class));
            return true;
        } else if (id == R.id.action_requisicoes) {
            startActivity(new Intent(MainActivity.this, RequisitionsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Inicia a intenção de captura de imagem
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("MainActivity", "Error occurred while creating the file", ex);
                Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.souzalima.fluxodeencomendas.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.e("MainActivity", "Photo file is null");
                Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("MainActivity", "No camera activity found");
            Toast.makeText(this, "Nenhuma atividade de câmera encontrada", Toast.LENGTH_SHORT).show();
        }
    }

    // Cria um arquivo de imagem
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Foto capturada", Toast.LENGTH_SHORT).show();
        }
    }

    // Faz o upload da imagem para o Firebase Storage
    private void uploadImageToFirebaseStorage(String imagePath, Order order, String orderId) {
        // Inicializar Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Criar uma referência para o arquivo a ser enviado
        Uri file = Uri.fromFile(new File(imagePath));
        StorageReference imagesRef = storageRef.child("images/" + file.getLastPathSegment());

        // Fazer o upload do arquivo
        UploadTask uploadTask = imagesRef.putFile(file);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Obter a URL de download da imagem
            imagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d("MainActivity", "Imagem enviada com sucesso. URL: " + downloadUrl);
                // Atualizar a URL da foto no Firebase Realtime Database
                order.setImageUrl(downloadUrl);
                database.child("orders").child(orderId).setValue(order);
                Toast.makeText(MainActivity.this, "Encomenda adicionada com sucesso", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e("MainActivity", "Erro ao enviar imagem", e);
            Toast.makeText(this, "Erro ao enviar imagem", Toast.LENGTH_SHORT).show();
        });
    }

    // Verifica e solicita permissões necessárias
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    REQUEST_PERMISSIONS);
        }
    }

    // Verifica se as permissões foram concedidas
    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão concedida", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Envia mensagem pelo WhatsApp
    private void sendWhatsAppMessage(String unit, String name, String date, String time, String description, String notaFiscal, String plantonista, @Nullable String photoPath) {
        String message = "Nova encomenda:\n" +
                "Unidade: " + unit + "\n" +
                "Nome: " + name + "\n" +
                "Data: " + date + "\n" +
                "Hora: " + time + "\n" +
                "Descrição: " + description + "\n" +
                "Nota Fiscal: " + notaFiscal + "\n" +
                "Plantonista: " + plantonista;

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");

        if (photoPath != null) {
            File photoFile = new File(photoPath);
            Uri photoURI = FileProvider.getUriForFile(this, "com.souzalima.fluxodeencomendas.fileprovider", photoFile);
            sendIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
            sendIntent.setType("image/jpeg");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        sendIntent.setPackage("com.whatsapp");

        try {
            startActivity(sendIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "WhatsApp não está instalado.", Toast.LENGTH_SHORT).show();
        }
    }
    private void clearFields() {
        editTextUnit.setText("");
        editTextName.setText("");
        editTextDescription.setText("");
        editTextNotaFiscal.setText("");
        spinnerPlantonista.setSelection(0);
        editTextOutroPlantonista.setVisibility(View.GONE);
        editTextOutroPlantonista.setText("");
    }
    // Classe interna para representar uma encomenda
    public static class Order {
        private String unit;
        private String name;
        private String date;
        private String time;
        private String description;
        private String plantonista;
        private String imageUrl;
        private String orderId;
        private String status;
        private String notaFiscal; // Novo campo adicionado

        public Order() {
            // Construtor vazio requerido pelo Firebase
        }

        // Métodos getters e setters
        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPlantonista() {
            return plantonista;
        }

        public void setPlantonista(String plantonista) {
            this.plantonista = plantonista;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotaFiscal() {
            return notaFiscal;
        }

        public void setNotaFiscal(String notaFiscal) {
            this.notaFiscal = notaFiscal;
        }
    }
}
