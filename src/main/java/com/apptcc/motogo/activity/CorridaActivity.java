package com.apptcc.motogo.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;


import com.apptcc.motogo.config.ConfiguracaoFirebase;

import com.apptcc.motogo.helper.UsuarioFirebase;
import com.apptcc.motogo.model.Destino;
import com.apptcc.motogo.model.Requisicao;
import com.apptcc.motogo.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;



import com.apptcc.motogo.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;

public class CorridaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Button buttonAceitarcorrida;
    private FloatingActionButton fabRota;


    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMototaxista;
    private LatLng localPassageiro;
    protected Usuario mototaxista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Marker marcadorMototaxista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private boolean requisicaoAtiva;
    private Destino destino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida);

        inicializarComponentes();

        //Recupera dados do usuário
        if( getIntent().getExtras().containsKey("idRequisicao")
                && getIntent().getExtras().containsKey("mototaxista") ){
            Bundle extras = getIntent().getExtras();
            mototaxista = (Usuario) extras.getSerializable("mototaxista");
            localMototaxista= new LatLng(
                    Double.parseDouble(mototaxista.getLatitude()),
                    Double.parseDouble(mototaxista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();
        }

    }



    private void verificaStatusRequisicao(){

        DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child( idRequisicao );
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Recupera requisição
                requisicao = dataSnapshot.getValue(Requisicao.class);

                if(requisicao != null){
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );
                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void alteraInterfaceStatusRequisicao(String status){

        switch ( status ){
            case Requisicao.STATUS_AGUARDANDO :
                requisicaoAguardando();
                break;
            case Requisicao.STATUS_A_CAMINHO :
                requisicaoACaminho();
                break;
            case Requisicao.STATUS_VIAGEM:
                requisicaoViagem();
                break;
            case Requisicao.STATUS_FINALIZADA:
                requisicaoFinalizada();
                break;
            case Requisicao.STATUS_CANCELADA :
                requisicaoCancelada();
                break;
        }

    }

    private void requisicaoCancelada(){

        Toast.makeText(this,
                "Requisição foi cancelada pelo passageiro!",
                Toast.LENGTH_SHORT).show();

        startActivity(new Intent(CorridaActivity.this, RequisicoesActivity.class));

    }
    private void centralizarMarcador(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 20)
        );
    }

    private void requisicaoAguardando(){

        buttonAceitarcorrida.setText("Aceitar corrida");

        //Exibe marcador do mototaxista
        adicionaMarcadorMototaxista(localMototaxista, mototaxista.getNome() );

        //Exibe marcador passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMototaxista, marcadorPassageiro);

    }

    @SuppressLint("RestrictedApi")
    private void requisicaoACaminho(){
        buttonAceitarcorrida.setText("A caminho do passageiro");
        fabRota.setVisibility(View.VISIBLE);
        Toast.makeText(this,
                "Clique no icone do usuario para ver o nome do passageiro ",
                Toast.LENGTH_LONG).show();


        //Exibe marcador do mototaxista
        adicionaMarcadorMototaxista(localMototaxista, mototaxista.getNome() );

        //Exibe marcador passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMototaxista, marcadorPassageiro);

        //Inicia monitoramento do motorista / passageiro
        iniciarmonitoramento(mototaxista, localPassageiro, Requisicao.STATUS_VIAGEM);

    }

    @SuppressLint("RestrictedApi")
    private void requisicaoViagem(){
        fabRota.setVisibility(View.VISIBLE);
        buttonAceitarcorrida.setText("A caminho do destino");

        //exibe marcador do motaxista
        adicionaMarcadorMototaxista(localMototaxista, mototaxista.getNome());

        //marcador destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())

        );
        adicionaMarcadorDestino(localDestino, "Destino");

        //centraliza macardores
        centralizarDoisMarcadores(marcadorMototaxista, marcadorDestino);

        //Inicia monitoramento do motorista / passageiro
        iniciarmonitoramento(mototaxista, localDestino, Requisicao.STATUS_FINALIZADA);

    }

    @SuppressLint("RestrictedApi")
    private void requisicaoFinalizada(){

        fabRota.setVisibility(View.GONE);
        requisicaoAtiva = false;

        if( marcadorMototaxista != null )
            marcadorMototaxista.remove();

        if( marcadorDestino != null )
            marcadorDestino.remove();
        //Exibe marcador destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );

        adicionaMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        buttonAceitarcorrida.setText("corrida finalizada");


        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Você chegou no destino")
                .setMessage("Você chegou no destino do passageiro")
                .setCancelable(false)
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();

                        finish();

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void iniciarmonitoramento(Usuario uOrigem, LatLng localDestino, String status){
        //inicializar geofire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //adiciona circulo no passageiro
        Circle circulo = mMap.addCircle(
                new CircleOptions()
                .center(localDestino)
                .radius(50)//metros
                .fillColor(Color.argb(90,255,153,0))
                .strokeColor(Color.argb(190,255,152,0))
        );

        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.05//em km (0.05 metros)
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
             if (key.equals(uOrigem.getId())){

                 //Log.d("onKeyEntered", "onKeyEntered: mototaxista está dentro da área!");

                 //altera status da requisição
                 requisicao.setStatus(status);
                 requisicao.atualizarStatus();

                 //remove listener
                 geoQuery.removeAllListeners();
                 circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });


    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include( marcador1.getPosition() );
        builder.include( marcador2.getPosition() );

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,largura,altura,espacoInterno)
        );

    }



    private void adicionaMarcadorMototaxista(LatLng localizacao, String titulo){

        if( marcadorMototaxista != null )
            marcadorMototaxista.remove();

        marcadorMototaxista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.mototaxi))
        );

    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo){

        if( marcadorPassageiro != null )
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );

    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo){

        if( marcadorPassageiro != null )
            marcadorPassageiro.remove();

        if( marcadorDestino != null )
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localizacao do usuário
        recuperarLocalizacaoUsuario();

    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMototaxista = new LatLng(latitude, longitude);

                //atualziar geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                mototaxista.setLatitude(String.valueOf(latitude));
                mototaxista.setLongitude(String.valueOf(longitude));
                requisicao.setMototaxista( mototaxista );
                requisicao.atualizarLocalizacaoMototaxista();

                alteraInterfaceStatusRequisicao(statusRequisicao);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //Solicitar atualizações de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }



    }

    public void aceitarCorrida(View view){

        //Configura requisicao
        requisicao = new Requisicao();
        requisicao.setId( idRequisicao );
        requisicao.setMototaxista( mototaxista );
        requisicao.setStatus( Requisicao.STATUS_A_CAMINHO );

        requisicao.atualizar();

    }

    private void inicializarComponentes(){

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar corrida");


        buttonAceitarcorrida = findViewById(R.id.buttonAceitarcorrida);
        //configurções iniciais
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //adiciona evento de clique no fabRota
        fabRota = findViewById(R.id.fabRota);
        fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = statusRequisicao;
                if (status!= null && !status.isEmpty()) {
                    String lat = "";
                    String lon = "";

                    switch (status) {
                        case Requisicao.STATUS_A_CAMINHO:
                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);
                            break;
                        case Requisicao.STATUS_VIAGEM:
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }

                    //abrir rota
                    String latlong = lat + "," + lon;
                    Uri uri = Uri.parse("google.navigation:q="+latlong+"&mode=d");
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    i.setPackage("com.google.android.apps.maps");
                    startActivity(i);

                }

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva){
            Toast.makeText(CorridaActivity.this,
                    "Necessário encerrar a requisição atual!",
                    Toast.LENGTH_SHORT).show();
        }else {
            Intent i = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            startActivity(i);
        }
        return false;
    }
}