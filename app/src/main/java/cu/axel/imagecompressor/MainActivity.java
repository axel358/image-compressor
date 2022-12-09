package cu.axel.imagecompressor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import android.os.Environment;
import id.zelory.compressor.Compressor;
import android.graphics.BitmapFactory;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private final int OPEN_REQUEST_CODE=4;
    private ContentResolver resolver;
    private ImageView iIv, cIv;
    private Uri openUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resolver = getContentResolver();
        iIv = findViewById(R.id.i_preview_iv);
        cIv = findViewById(R.id.c_preview_iv);
    }

    public void open(View v) {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);
    }

    public void compress(View v) {
        try {
            InputStream is = resolver.openInputStream(openUri);
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp.jpg");
            OutputStream os = new FileOutputStream(file);
            byte buffer[] = new byte[1024];

            while (is.read(buffer) != -1) {
                os.write(buffer);   
            }
            is.close();
            os.close();
            
            File compressed = new Compressor(this).setQuality(60).setDestinationDirectoryPath(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"Compressed").getAbsolutePath()).compressToFile(file);
            iIv.setImageBitmap(BitmapFactory.decodeFile(compressed.getAbsolutePath()));
            
        } catch (IOException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                openUri = data.getData();
                resolver.takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                iIv.setImageURI(openUri);
            }
        }
    }



}
