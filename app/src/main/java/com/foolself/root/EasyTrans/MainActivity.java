package com.foolself.root.EasyTrans;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private EditText editText;
    private TextView reqText;
    private TextView textView;
    private ImageButton delete;
    private ImageButton submit;
    private android.content.ClipboardManager clipboardManager;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            textView.setText(msg.obj + "");
        }
    };

    Runnable newThread = new Runnable() {
        String result;

        @Override
        public void run() {
            String input = editText.getText().toString();
            try {
                result = getResult(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Message msg = handler.obtainMessage();
            msg.obj = result;
            handler.sendMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.input);
        reqText = (TextView) findViewById(R.id.req);
        textView = (TextView) findViewById(R.id.result);
        delete = (ImageButton) findViewById(R.id.delete);
        submit = (ImageButton) findViewById(R.id.submit);
        clipboardManager  = (android.content.ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboardManager.hasPrimaryClip()) {
            editText.setText(clipboardManager.getPrimaryClip().getItemAt(0).getText());
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

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText("");
            }
        });

    }

    private String getResult(String input) throws IOException {
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
                return arr2[0];
            }
            else {
                String text = URLDecoder.decode(arr2[0], "utf-8");
                text = convert(text);
                return text;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "something error";
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
}
