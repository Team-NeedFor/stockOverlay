package View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.cdst.stockoverlay.R;

import Model.User;
import Module.DBA;
import View.Dialog.NicknameDialog;
import ViewModel.MainViewModel;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private SignInButton Google_Login;
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInOptions gso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 로그인 관련 기초 변수들 초기화
        mAuth = FirebaseAuth.getInstance();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1055706717038-k008naroeutrj0otfhs05cgft4dt9srp.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        setContentView(R.layout.activity_login);
        connectGoogleLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 로그인 세션 여부 확인
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            User user = new User();
            if(new DBA().isInitNickname(getDatabasePath("User"))) {
                NicknameDialog nicknameDialog = new NicknameDialog(LoginActivity.this);
                nicknameDialog.callFunction(user, LoginActivity.this);
            } else {
                new DBA().initNickname(getDatabasePath("User"), user, new DBA().getNickname(getDatabasePath("User")));
                new DBA().initInterestedStocks(getDatabasePath("User"), user);
                if(user.getInterestedStocks().size() != 0) new MainViewModel().initStockList(getAssets(), user.getInterestedStocks());

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        }
    }

    // -------------- 구글 로그인 메서드 -------------
    public void connectGoogleLogin() {
        // 구글 로그인 연동
        Google_Login = findViewById(R.id.Google_Login);
        Google_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent,RC_SIGN_IN);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(getApplicationContext(), "Login Error: " + result.getStatus().toString(), Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getApplicationContext(), "Login Error: 구글계정 연동 실패.", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(),null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "인증 실패" + task.getResult().toString(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "구글 로그인 인증 성공", Toast.LENGTH_SHORT).show();

                            User user = new User();
                            if(new DBA().isInitNickname(getDatabasePath("User"))) {
                                NicknameDialog nicknameDialog = new NicknameDialog(LoginActivity.this);
                                nicknameDialog.callFunction(user, LoginActivity.this);
                            } else {
                                new DBA().initNickname(getDatabasePath("User"), user, new DBA().getNickname(getDatabasePath("User")));
                                new DBA().initInterestedStocks(getDatabasePath("User"), user);
                                if(user.getInterestedStocks().size() != 0) new MainViewModel().initStockList(getAssets(), user.getInterestedStocks());

                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }
}