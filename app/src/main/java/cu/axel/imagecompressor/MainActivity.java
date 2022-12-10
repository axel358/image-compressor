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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.graphics.Bitmap;
import java.text.DecimalFormat;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends AppCompatActivity {
    private final int OPEN_REQUEST_CODE=4;
    private ContentResolver resolver;
    private ImageView previewIv;
    private Uri openUri;
    private TextView infoTv;
    private File openFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resolver = getContentResolver();
        previewIv = findViewById(R.id.preview_iv);
        infoTv = findViewById(R.id.image_info_tv);
    }

    public void open(View v) {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);
    }
    
    public void showOptions(View v){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_options, null);
        dialog.setTitle("Compression options");
        dialog.setView(view);
        dialog.setPositiveButton("Apply", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                }
            });
        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

    public void compress(View v) {
        try {
            File compressedFile = new Compressor(this)
            .setQuality(60)
            .setCompressFormat(Bitmap.CompressFormat.WEBP)
            .setDestinationDirectoryPath(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Compressed").getAbsolutePath())
            .compressToFile(openFile);
            Bitmap bitmap = BitmapFactory.decodeFile(compressedFile.getAbsolutePath());
            infoTv.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + " " + formatFileSize(compressedFile.length()));
            previewIv.setImageBitmap(bitmap);

        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                openUri = data.getData();
                resolver.takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    InputStream is = resolver.openInputStream(openUri);
                    openFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp.png");
                    OutputStream os = new FileOutputStream(openFile);
                    byte buffer[] = new byte[1024];

                    while (is.read(buffer) != -1) {
                        os.write(buffer);   
                    }
                    is.close();
                    os.close();
                    
                    Bitmap bitmap = BitmapFactory.decodeFile(openFile.getAbsolutePath());
                    previewIv.setImageBitmap(bitmap);
                    infoTv.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + " " + formatFileSize(openFile.length()));
                } catch (IOException e) {}
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_image:
                open(null);
        }
        return super.onOptionsItemSelected(item);
    }

    public static String formatFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
