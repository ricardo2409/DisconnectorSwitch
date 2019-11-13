package com.example.cuchillas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static java.lang.Long.toBinaryString;
import static java.lang.Long.valueOf;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    TextView tvVolt, tvEstado;
    Button btnConnect, btnAbrir, btnCerrar, btnPassword;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_REQUEST_CODE =101;
    private GoogleMap mMap;
    private Task<Location> task;

    boolean connected = false;
    String cadena = "F1 00 0F 00 D2 21 00 10 00 00 00 32 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F2";
    private static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static BluetoothDevice device;
    public static BluetoothSocket socket;
    public static OutputStream outputStream;
    public static InputStream inputStream;

    static Button btnDiagnostico;
    static Handler handler = new Handler();


    static String tokens[];
    static String tokensRSSI[];

    static boolean socketConectado;
    boolean stopThread;

    static String s;

    static String a;
    static String b;

    static Thread thread;
    StringBuilder sb, sbAux;
    int control;
    String uno, dos;
    Boolean boolDos;
    boolean boolPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initItems();
        //readString(cadena);//Prueba sin BT
        /*
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }else{
            task = fusedLocationProviderClient.getLastLocation();
            fetchLastLocation();
        }
        */
    }
    public void initItems(){
        tvVolt = (TextView)findViewById(R.id.tvVolts);
        tvEstado = (TextView)findViewById(R.id.tvEstado);
        btnConnect = (Button)findViewById(R.id.btnConectar);
        btnAbrir = (Button)findViewById(R.id.btnAbrir);
        btnCerrar = (Button)findViewById(R.id.btnCerrar);
        btnPassword = (Button)findViewById(R.id.btnPassword);

        btnAbrir.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        btnCerrar.setOnClickListener(this);
        btnPassword.setOnClickListener(this);

    }

    public void print(String message){
        System.out.println(message);
    }

    //Identifica el device BT
    public boolean BTinit()
    {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            Toast.makeText(getApplicationContext(), "Este dispositivo no soporta bluetooth", Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter,0);
            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            Toast.makeText(getApplicationContext(), "Favor de conectar un dispositivo", Toast.LENGTH_SHORT).show();
        }
        else
        {
            for(BluetoothDevice iterator : bondedDevices)
            {

                //Suponiendo que solo haya un bondedDevice
                device = iterator;
                found = true;
                //Toast.makeText(getApplicationContext(), "Conectado a: " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
        return found;
    }
    //Conexión al device BT
    public boolean BTconnect()
    {
        try
        {
            conectar();

        }
        catch(IOException e)
        {
            Toast.makeText(getApplicationContext(), "Conexión no exitosa", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            connected = false;
        }

        return connected;
    }

    public void conectar() throws IOException{
        socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Crea un socket para manejar la conexión
        socket.connect();
        socketConectado = true;
        Log.d("Socket ", String.valueOf(socket.isConnected()));
        Toast.makeText(getApplicationContext(), "Conexión exitosa", Toast.LENGTH_SHORT).show();
        connected = true;
        btnConnect.setText("Desconectar módulo Bluetooth");
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        beginListenForData();

        //waitMs(5000);
        //closeSocket();

    }

    public void desconectarBluetooth() throws IOException{
        //Desconectar bluetooth
        if(socketConectado){
            System.out.println("Socket Conectado");
            outputStream.close();
            outputStream = null;
            inputStream.close();
            inputStream = null;
            socket.close();
            socket = null;
        }
        resetFields();
        connected = false;
        btnConnect.setText("Conectar a módulo Bluetooth");
        device = null;
        stopThread = true;
        socketConectado = false;
        boolPassword = false; //Para que lo vuelva a pedir la siguiente vez que se conecte a otro equipo
    }

    void beginListenForData() {
        stopThread = false;
        final String[] uno = {null};
        final String[] dos = { null };
        thread = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        //waitMs(1000);
                        final int byteCount = inputStream.available();
                        if(byteCount > 0) {


                            byte[] packetBytes = new byte[byteCount];
                            inputStream.read(packetBytes);

                            sb = new StringBuilder();
                            for (byte b : packetBytes) {
                                sb.append(String.format("%02X ", b));
                                //print("Esto tiene en el append: " + sb.toString());
                            }
                            if(control == 0){
                                uno[0] = sb.toString();
                                print("Uno: " + uno[0]);
                            }else{
                                dos[0] = sb.toString();
                                print("Dos: " + dos[0]);
                                print("Esto es todo completo: " + uno[0] + dos[0]);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Status
                                        readString(uno[0] + dos[0]); //String final que se lee y analiza

                                    }
                                });
                            }
                            System.out.println("Hex as string: " + sb.toString()); //Hex as string
                            if(sb.toString().contains("F1") && !sb.toString().contains("F2")){
                                print("Tiene F1 pero no F2");
                                control = 1;
                            }else if(!sb.toString().contains("F1") && sb.toString().contains("F2")){
                                print("Tiene F2 pero no F1");
                                control = 0;
                                boolDos = true;
                                //print(uno + dos);

                            }else if(sb.toString().contains("F1") && sb.toString().contains("F2")){
                                //Mandarlo asi como está
                                print("Ya estaba completo: " + sb.toString());
                            }

                        }

                    }
                    catch (IOException ex) {
                        stopThread = true;
                    }
                }
                System.out.println("Stop thread es true");
            }
        });
        thread.start();
    }
    public void readString(String message){
        String [] arrOfStr = message.split(" ");

        Integer[] intarray=new Integer[arrOfStr.length];
        int i=0;
        for (String str : arrOfStr){
            int temp1 = Integer.parseInt(arrOfStr[i].trim(), 16 );//De hex a decimal
            intarray[i] = temp1;
            print("Esto es lo que convierto: " + i + " " + temp1);
            i++;
        }

        if(Arrays.asList(arrOfStr).contains("F0")){
            print("Si tiene F0");
            //convertString(arrOfStr);

        }else{
            print("No tiene F0");
            String pattern="00000000";
            DecimalFormat myFormatter = new DecimalFormat(pattern);
            String binaria1 = myFormatter.format(valueOf(toBinaryString(intarray[7])));
            String binaria2 = myFormatter.format(valueOf(toBinaryString(intarray[8])));

            print("Esto es lo que tiene el 7:" + intarray[7]);
            switch (intarray[7]) {
                case 00:
                    tvEstado.setText("Abierto");
                    break;
                case 16:
                    tvEstado.setText("Cerrado");
                    break;
                case 32:
                    tvEstado.setText("En Transición");
                    break;

            }
            tvVolt.setText(String.valueOf(Double.valueOf(intarray[12]*256 + intarray[11])/100) + " Volts");
        }
    }

    public void resetFields(){
       tvVolt.setText("");
       tvEstado.setText("");
    }

    void sendAbrir() throws IOException
    {
        System.out.println("Estoy en el Abrir");
        String msg = "$SWITCH_OPEN&";
        outputStream.write(msg.getBytes());
    }
    void sendCerrar() throws IOException
    {
        System.out.println("Estoy en el Cerrar");
        String msg = "$SWITCH_CLOSE&";
        outputStream.write(msg.getBytes());
    }
    //Input Dialog para ingresar el password para permitir el cambio a remoto
    public void showPasswordDialog(final String title, final String message){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(getApplicationContext());
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        edittext.setTextColor(getResources().getColor(R.color.black));
        edittext.setRawInputType(Configuration.KEYBOARD_12KEY);
        alert.setMessage(message);
        alert.setTitle(title);
        alert.setView(edittext);

        alert.setPositiveButton("Ingresar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String pass = edittext.getText().toString();
                //Pasar el pass con este comando = $PASS=12345,& regresa OK si es correcto o error si incorrecto
                try{
                    sendPassword(pass);
                }catch(IOException e){

                }

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cerrar el input dialog
                dialog.dismiss();
                //Regresate a Remoto

            }
        });

        alert.show();
    }
    //Función que manda el password a la tarjeta y poder hacer controles
    void sendPassword(String pass) throws IOException
    {

        System.out.println("Estoy en el sendPassword");
        String msg = "$PASS=" + pass + ",& ";
        outputStream.write(msg.getBytes());
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConectar:
                if (!connected) {
                    if (BTinit()) {
                        BTconnect();
                    }
                } else {
                    try {
                        desconectarBluetooth();
                    } catch (IOException ex) {
                    }
                }
                break;
            case R.id.btnAbrir:
                if (connected) {
                    try {
                        sendAbrir();
                    } catch (IOException ex) {
                    }

                } else {
                    showToast("Bluetooth Desconectado");
                }
                break;
            case R.id.btnCerrar:
                if (connected) {
                    try {
                        sendCerrar();
                    } catch (IOException ex) {
                    }

                } else {
                    showToast("Bluetooth Desconectado");
                }
                break;
            case R.id.btnPassword:
                if (connected) {

                    showPasswordDialog("Ingrese el Password", "");


                } else {
                    showToast("Bluetooth Desconectado");
                }
                break;
        }
    }
    private void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void fetchLastLocation(){
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    System.out.println("Esto es latitude y longitude: " + currentLocation.getLatitude()+" "+currentLocation.getLongitude());
                    //Toast.makeText(MainActivity.this,"Esto es latitude y longitude: " + currentLocation.getLatitude()+" "+currentLocation.getLongitude(),Toast.LENGTH_SHORT).show();
                    SupportMapFragment supportMapFragment= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    supportMapFragment.getMapAsync(MainActivity.this);
                }else{
                    SupportMapFragment supportMapFragment= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    supportMapFragment.getMapAsync(MainActivity.this);
                    Toast.makeText(MainActivity.this,"No Location recorded",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("OnMapReady");
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }else{
            mMap.setMyLocationEnabled(true);
        }


    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                    mMap.setMyLocationEnabled(true);

                } else {
                    Toast.makeText(MainActivity.this,"Location permission missing",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


}
