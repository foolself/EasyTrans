package com.foolself.root.EasyTrans;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public DBManager dbHelper;
    private SQLiteDatabase dictionary;
    private EditText editText;
    private TextView reqText;
    private ImageButton delete;
    private ImageButton submit;
    private Switch net_ctrl;
    private android.content.ClipboardManager clipboardManager;

    ListView listView;
    private SimpleAdapter list_adapter;
    private List<Map<String, Object>> list_data;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            list_adapter.notifyDataSetChanged();
        }
    };

    Runnable newThread = new Runnable() {

        @Override
        public void run() {
            String input = editText.getText().toString();
            if (!checkNetworkState() || !net_ctrl.isChecked()) {
                getResultFromLoc(input);
            }
            else {
                try {
                    getResultFromNet(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Message msg = handler.obtainMessage();
            handler.sendMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String DBFilePath = DBManager.DB_PATH + "/" + DBManager.DB_NAME;
        if (!fileIsExists(DBFilePath)) {
            dbHelper = new DBManager(this);
            dbHelper.openDatabase();
            dbHelper.closeDatabase();
        }
        dictionary = SQLiteDatabase.openOrCreateDatabase(DBFilePath, null);

        editText = (EditText) findViewById(R.id.input);
        reqText = (TextView) findViewById(R.id.req);
        delete = (ImageButton) findViewById(R.id.delete);
        submit = (ImageButton) findViewById(R.id.submit);
        net_ctrl = (Switch) findViewById(R.id.net_ctrl);
        clipboardManager  = (android.content.ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

        listView = (ListView) findViewById(R.id.items);
        list_data = new ArrayList<Map<String, Object>>();
        list_adapter = new SimpleAdapter(this, list_data, R.layout.item,
                new String[]{"word", "explain"}, new int[] {R.id.word, R.id.explain});
        listView.setAdapter(list_adapter);


        if (clipboardManager.hasPrimaryClip()) {
            editText.setText(clipboardManager.getPrimaryClip().getItemAt(0).getText());
            String temp = clipboardManager.getPrimaryClip().getDescription().toString();
            Log.i("tag", "clip: " + temp);
        }

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editText.getText().toString().equals("")) {
                    reqText.setText(editText.getText().toString() + " :");
                    Thread t = new Thread(newThread);
                    t.start();
                }
            }
        });

        net_ctrl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    net_ctrl.setText("在线");
                else
                    net_ctrl.setText("离线");
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText("");
            }
        });

    }

    private void getResultFromNet(String input) throws IOException {
//        String q = URLEncoder.encode(rq, "utf-8");

        String q = input;
        String from = "zh";
        String to = "en";
        if (q.charAt(0) < 'z' && q.charAt(0) > 'A') {
            from = "en";
            to = "zh";
        }

        String appId = "appId";
        String token = "token";
        String mainUrl = "http://api.fanyi.baidu.com/api/trans/vip/translate";
        Random random = new Random();
        int salt = random.nextInt(10000);
        // 对appId+源文+随机数+token计算md5值
        StringBuilder md5String = new StringBuilder();
        md5String.append(appId).append(q).append(salt).append(token);
        // String md5 = DigestUtils.md5Hex(data);
         String md5 = new String(Hex.encodeHex(DigestUtils.md5(md5String.toString())));
//        String md5 = DigestUtils.md5Hex(md5String.toString());

        String urlString = mainUrl + "?q=" + q + "&from=" + from + "&to=" + to + "&appid=" + appId + "&salt=" + salt + "&sign=" + md5;

        try {
            StringBuffer html = new StringBuffer();
            URL url = new URL(urlString); //根据 String 表示形式创建 URL 对象。
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();// 返回一个 URLConnection 对象，它表示到 URL 所引用的远程对象的连接。
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36");
            conn.setRequestProperty("content-type", "application/x-javascript; charset=UTF-8");
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());//返回从此打开的连接读取的输入流。
            BufferedReader br = new BufferedReader(isr);//创建一个使用默认大小输入缓冲区的缓冲字符输入流。

            String temp;
            while ((temp = br.readLine()) != null) { //按行读取输出流
                if(!temp.trim().equals("")){
                    html.append(temp).append("\n"); //读完每行后换行
                }
            }
            br.close(); //关闭
            isr.close(); //关闭
            String result = html.toString();
            Log.i("tag", "response: " + result);
            String[] arr1 = result.split("dst\":\"");
            String[] arr2 = arr1[1].split("\"");
            if (from.equals("zh")) {
                result = arr2[0];
            }
            else {
                String text = URLDecoder.decode(arr2[0], "utf-8");
                result = convert(text);
            }

            list_data.clear();
            Map<String, Object> listem = new HashMap<String, Object>();
            listem.put("word", "");
            listem.put("explain", result);
            list_data.add(listem);
        } catch (Exception e) {
            e.printStackTrace();
            list_data.clear();
            Map<String, Object> listem = new HashMap<String, Object>();
            listem.put("word", "");
            listem.put("explain", "Something Error");
            list_data.add(listem);
        }
    }

    private void getResultFromLoc(String input) {
        String result = "";
        String key = "explain";
        Cursor cur;
        if (input.charAt(0) < 'z' && input.charAt(0) > 'A') {
            cur = dictionary.rawQuery("select * from Words where word=?", new String[]{input});
        }
        else {
            cur = dictionary.rawQuery("select * from Words where explain like ?", new String[]{"%" + input + "%"});
        }
        list_data.clear();
        if (cur != null) {
            int NUM_R = cur.getCount();
            if (cur.moveToFirst()) {
                do {
                    result = result + cur.getString(cur.getColumnIndex("word")) + ": \n  ";
                    result = result + cur.getString(cur.getColumnIndex("explain")) + "\n\n";

                    Map<String, Object> listem = new HashMap<String, Object>();
                    listem.put("word",
                            cur.getString(cur.getColumnIndex("word")));
                    listem.put("explain",
                            cur.getString(cur.getColumnIndex("explain")));
                    list_data.add(listem);
                } while (cur.moveToNext());
            }
        }
    }
    public String convert(String utfString){
        StringBuilder sb = new StringBuilder();
        int i = -1;
        int pos = 0;
        while((i=utfString.indexOf("\\u", pos)) != -1){
            sb.append(utfString.substring(pos, i));
            if(i+5 < utfString.length()){
                pos = i+6;
                sb.append((char)Integer.parseInt(utfString.substring(i+2, i+6), 16));
            }
        }
        return sb.toString();
    }

    private boolean checkNetworkState() {
        boolean flag = false;
        //得到网络连接信息
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        return flag;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dictionary != null && dictionary.isOpen()) {
            dictionary.close();
        }
    }

    public boolean fileIsExists(String filePath){
        try{
            File f=new File(filePath);
            if(f.exists()){
                Log.i("tag", "exist");
                return true;
            }

        }catch (Exception e) {
            return false;
        }
        return false;
    }
}
