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
import android.graphics.BitmapFactory;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.graphics.Bitmap;
import java.text.DecimalFormat;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.io.ByteArrayOutputStream;
import android.graphics.Matrix;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.widget.RadioButton;
import android.graphics.Bitmap.CompressFormat;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends AppCompatActivity {
    private final int OPEN_REQUEST_CODE=4;
    private ContentResolver resolver;
    private ImageView previewIv;
    private Uri openUri;
    private TextView infoTv;
    private File openFile;
    private SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resolver = getContentResolver();
        previewIv = findViewById(R.id.preview_iv);
        infoTv = findViewById(R.id.image_info_tv);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void open(View v) {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);
    }

    public void showOptions(View v) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final View view = getLayoutInflater().inflate(R.layout.dialog_options, null);
        dialog.setTitle("Compression options");
        dialog.setView(view);
        dialog.setNegativeButton("Cancel", null);

        final RadioGroup formatGroup = view.findViewById(R.id.group_formats);
        final SeekBar qualitySb = view.findViewById(R.id.sb_quality);
        final SeekBar resSb = view.findViewById(R.id.sb_resolution);
        final TextView resTv = view.findViewById(R.id.tv_resolution);
        final TextView qualityTv = view.findViewById(R.id.tv_quality);
        
        qualitySb.setMax(100);
        resSb.setMax(100);
        qualitySb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    qualityTv.setText(p2+"");
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                }
            });
        resSb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    resTv.setText(p2+"");
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                }
            });
        resSb.setProgress(sp.getInt("resolution", 90));
        qualitySb.setProgress(sp.getInt("quality", 90));
        RadioButton webpBtn = view.findViewById(R.id.btn_webp);
        RadioButton jpgBtn = view.findViewById(R.id.btn_jpg);
        
        String format = sp.getString("format", "jpg");
        webpBtn.setChecked(format.equals("webp"));
        jpgBtn.setChecked(format.equals("jpg"));

        dialog.setPositiveButton("Apply", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putInt("resolution", resSb.getProgress());
                    editor.putInt("quality", qualitySb.getProgress());
                    RadioButton checkedBtn = view.findViewById(formatGroup.getCheckedRadioButtonId());
                    editor.putString("format", checkedBtn.getText().toString().toLowerCase());
                    editor.commit();
                }
            });

        dialog.show();
    }

    public void compress(View v) {
        try {

            Bitmap bitmap = BitmapFactory.decodeFile(openFile.getAbsolutePath());
            bitmap = scaleBitmap(bitmap, sp.getInt("resolution", 100) * 0.01f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            
            Bitmap.CompressFormat format = sp.getString("format", "jpg").equals("jpg") ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.WEBP;
            
            bitmap.compress(format, sp.getInt("quality", 100), bytes);

            File compressedFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "out.webp");
            compressedFile.createNewFile();
            FileOutputStream os = new FileOutputStream(compressedFile);
            os.write(bytes.toByteArray());
            os.close();
            infoTv.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + " " + formatFileSize(compressedFile.length()));
            previewIv.setImageBitmap(bitmap);

        } catch (Exception e) {
            Toast.makeText(this, e.toString()+"daas", Toast.LENGTH_LONG).show();
        }


    }

    public Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
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
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
