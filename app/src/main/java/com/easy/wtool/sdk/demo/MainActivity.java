package com.easy.wtool.sdk.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.easy.wtool.sdk.MessageEvent;
import com.easy.wtool.sdk.OnMessageListener;
import com.easy.wtool.sdk.WToolSDK;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static String LOG_TAG = "javahook";
    private static String DEF_TALKER = "接收人(点击选择)";
    private static String DEF_IMAGEFILE = "图片(点击选择)";
    private static String DEF_VOICEFILE = "语音(点击选择)";
    private static String DEF_VIDEOFILE = "视频(点击选择)";
    private static int RESULT_IMAGE = 1;
    private static int RESULT_VOICE = 2;
    private static int RESULT_VIDEO = 3;
    Context mContext;
    // Used to load the 'native-lib' library on application startup.

    private ConfigUtils configUtils;
    private String toWxId = "";
    private String toImageFile = "";
    private String toVoiceFile = "";
    private String toVideoFile = "";
    private int selectedWxIdIndex = 0;
    TextView labelImageFile,labelVoiceFile,labelVideoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;

        final WToolSDK wToolSDK = new WToolSDK();
        this.setTitle(this.getTitle() + " - V" + wToolSDK.getVersion());

        configUtils = new ConfigUtils(this);


        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
        Button buttonInit = (Button) findViewById(R.id.buttonInit);
        Button buttonText = (Button) findViewById(R.id.buttonText);
        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        Button buttonVoice = (Button) findViewById(R.id.buttonVoice);
        Button buttonVideo = (Button) findViewById(R.id.buttonVideo);
        Button buttonFriends = (Button) findViewById(R.id.buttonFriends);
        Button buttonChatrooms = (Button) findViewById(R.id.buttonChatrooms);
        final RadioButton radioButtonFriend = (RadioButton)findViewById(R.id.radioButtonFriend);
        final RadioButton radioButtonChatroom = (RadioButton)findViewById(R.id.radioButtonChatroom);
        final TextView labelWxid = (TextView) findViewById(R.id.labelWxid);
        labelWxid.setText(DEF_TALKER);
        labelImageFile = (TextView) findViewById(R.id.labelImageFile);
        labelImageFile.setText(DEF_IMAGEFILE);
        labelVoiceFile = (TextView) findViewById(R.id.labelVoiceFile);
        labelVoiceFile.setText(DEF_VOICEFILE);
        labelVideoFile = (TextView) findViewById(R.id.labelVideoFile);
        labelVideoFile.setText(DEF_VIDEOFILE);
        final Button buttonStartMessage = (Button) findViewById(R.id.buttonStartMessage);
        buttonStartMessage.setVisibility(View.INVISIBLE);
        final EditText editAuthCode = (EditText) findViewById(R.id.editAuthCode);

        final EditText editText = (EditText) findViewById(R.id.editText);

        final TextView editContent = (TextView) findViewById(R.id.editContent);
        editAuthCode.setText(configUtils.get(ConfigUtils.KEY_AUTHCODE, "0279C8C340306804E57499CD112EB094CB13037A"));
        if (!editAuthCode.getText().toString().equals("")) {
            //初始化
            parseResult(wToolSDK.init(editAuthCode.getText().toString()));
        }


        editContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        //处理消息 回调的Handler
        final Handler messageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {


                MessageEvent event = (MessageEvent) msg.obj;

                editContent.append("message: " + event.getTalker() + "," + event.getContent() + "\n");
                super.handleMessage(msg);
            }
        };
        wToolSDK.setOnMessageListener(new OnMessageListener() {
            @Override
            public void messageEvent(MessageEvent event) {
                Log.d(LOG_TAG, "on message: " + event.getTalker() + "," + event.getContent());

                //editContent.setText("message: "+event.getTalker()+","+event.getContent());
                //由于该回调是在线程中，因些如果是有UI更新，需要使用Handler
                messageHandler.obtainMessage(0, event).sendToTarget();

            }
        });
        buttonInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editAuthCode.getText().toString().equals("")) {
                    Toast.makeText(mContext, "授权码不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                //初始化
                parseResult(wToolSDK.init(editAuthCode.getText().toString()));
                configUtils.save(ConfigUtils.KEY_AUTHCODE, editAuthCode.getText().toString());
            }
        });
        labelWxid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                //builder.setIcon(R.drawable.ic_launcher);
                builder.setTitle(radioButtonFriend.isChecked()? "选择接收好友":"选择接收群");
                String content;
                if(radioButtonFriend.isChecked())
                {
                    content = wToolSDK.getFriends(0, 0);
                }
                else
                {
                    content = wToolSDK.getChatrooms(0, 0,false);
                }

                String text = "";
                try {
                    if (selectedWxIdIndex < 0) {
                        selectedWxIdIndex = 0;
                    }
                    final JSONObject jsonObject = new JSONObject(content);
                    if (jsonObject.getInt("result") == 0) {
                        final JSONArray jsonArray = jsonObject.getJSONArray("content");
                        if (jsonArray.length() > 0) {
                            final String[] friends = new String[jsonArray.length()];
                            for (int i = 0; i < jsonArray.length(); i++) {
                                friends[i] = jsonArray.getJSONObject(i).getString("nickname");
                                if(radioButtonChatroom.isChecked() && friends[i].equals(""))
                                {
                                    if(jsonArray.getJSONObject(i).has("displayname")) {
                                        friends[i] = jsonArray.getJSONObject(i).getString("displayname");
                                        if(friends[i].length()>20) {
                                            friends[i] = friends[i].substring(0, 15) + "...";
                                        }
                                    }
                                }
                            }
                            if (selectedWxIdIndex >= jsonArray.length()) {
                                selectedWxIdIndex = 0;
                            }
                            //    设置一个单项选择下拉框
                            /**
                             * 第一个参数指定我们要显示的一组下拉多选框的数据集合
                             * 第二个参数代表哪几个选项被选择，如果是null，则表示一个都不选择，如果希望指定哪一个多选选项框被选择，
                             * 需要传递一个boolean[]数组进去，其长度要和第一个参数的长度相同，例如 {true, false, false, true};
                             * 第三个参数给每一个多选项绑定一个监听器
                             */

                            builder.setSingleChoiceItems(friends, selectedWxIdIndex, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    selectedWxIdIndex = which;
                                }
                            });
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        toWxId = jsonArray.getJSONObject(selectedWxIdIndex).getString("wxid");
                                        labelWxid.setText(DEF_TALKER + "：" + friends[selectedWxIdIndex]);
                                    } catch (Exception e) {
                                        toWxId = "";
                                    }

                                }
                            });
                            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            builder.show();
                        } else {
                            text = radioButtonFriend.isChecked()?"无好友":"无群";
                        }
                    } else {
                        text = jsonObject.getString("errmsg");
                    }
                } catch (Exception e) {
                    text = "解析结果失败>>"+e.toString();
                    Log.e(LOG_TAG, "jsonerr", e);
                }
                if (text.length() > 0) {
                    Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
                }


            }

        });
        buttonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toWxId.equals("")) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (editText.getText().toString().equals("")) {
                    Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送文本
                parseResult(wToolSDK.sendText(toWxId, editText.getText().toString()));

            }
        });
        labelImageFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();//Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, RESULT_IMAGE);
            }
        });
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toWxId.equals("")) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (toImageFile.equals("")) {
                    Toast.makeText(mContext, "请选择要发送的图片！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送图片
                parseResult(wToolSDK.sendImage(toWxId, toImageFile));

            }
        });
        labelVoiceFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();//Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("audio/*");

                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, RESULT_VOICE);
            }
        });
        buttonVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toWxId.equals("")) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (toVoiceFile.equals("")) {
                    Toast.makeText(mContext, "请选择要发送的语音文件！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送图片
                parseResult(wToolSDK.sendVoice(toWxId, toVoiceFile,60));

            }
        });
        labelVideoFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();//Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, RESULT_VIDEO);
            }
        });
        buttonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toWxId.equals("")) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (toVideoFile.equals("")) {
                    Toast.makeText(mContext, "请选择要发送的视频文件！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送图片
                String thumbFile = makeVideoThumbFile(toVideoFile);
                if(thumbFile.equals(""))
                {
                    Toast.makeText(mContext, "生成视频缩略图失败！", Toast.LENGTH_LONG).show();
                    return;
                }
                parseResult(wToolSDK.sendVideo(toWxId, toVideoFile,thumbFile,60));

            }
        });
        buttonFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取好友列表
                String content = wToolSDK.getFriends(0, 0);
                editContent.setText(content);
                parseResult(content);
            }
        });
        buttonChatrooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取群列表
                String content = wToolSDK.getChatrooms(0, 0,true);
                editContent.setText(content);
                parseResult(content);
            }
        });
        buttonStartMessage.setTag(0);
        //buttonStartMessage.setVisibility(View.INVISIBLE);
        buttonStartMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonStartMessage.getTag().equals(0)) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(1);
                        jsonArray.put(2);
                        jsonObject.put("talkertypes", jsonArray);
                        jsonObject.put("froms", new JSONArray());
                        jsonArray = new JSONArray();
                        jsonArray.put(1);
                        jsonArray.put(42);
                        jsonObject.put("msgtypes", jsonArray);
                        jsonObject.put("msgfilters", new JSONArray());
                        String result = wToolSDK.startMessageListener(jsonObject.toString());
                        jsonObject = new JSONObject(result);
                        if (jsonObject.getInt("result") == 0) {
                            buttonStartMessage.setTag(1);
                            buttonStartMessage.setText("停止监听消息");
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "err", e);
                    }
                } else {
                    wToolSDK.stopMessageListener();
                    buttonStartMessage.setTag(0);
                    buttonStartMessage.setText("监听消息");
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null) {
            String v_path = "";
            Cursor cursor = null;
            try {
                try {

                    Uri uri = data.getData();
                    cursor = getContentResolver().query(uri, null, null,
                            null, null);
                    cursor.moveToFirst();
                    // String imgNo = cursor.getString(0); // 图片编号
                    v_path = cursor.getString(1); // 图片文件路径
                    //String v_size = cursor.getString(2); // 图片大小
                    //String v_name = cursor.getString(3); // 图片文件名
                    cursor.close();


                } catch (Exception e) {
                    //e.printStackTrace();
                    //Toast.makeText(mContext, "获取文件出错！", Toast.LENGTH_LONG).show();
                    try
                    {
                        Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
                        v_path = uri.getPath().toString();
                    }
                    catch (Exception e1)
                    {
                        Toast.makeText(mContext, "获取文件出错！", Toast.LENGTH_LONG).show();
                        return;
                    }

                }
            }
            finally {
                if(cursor!=null)
                {
                    try {
                        cursor.close();
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
            if (requestCode == RESULT_IMAGE) {

                /*
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                labelImageFile.setText(DEF_IMAGEFILE + "：" + picturePath);
                toImageFile = picturePath;
                */

                //LogUtil.e("v_path="+v_path);
                //LogUtil.e("v_size="+v_size);
                //LogUtil.e("v_name="+v_name);
                toImageFile = v_path;
                labelImageFile.setText(DEF_IMAGEFILE + "：" + toImageFile);


            }
            else if (requestCode == RESULT_VOICE) {


                toVoiceFile = v_path;
                labelVoiceFile.setText(DEF_VOICEFILE + "：" + toVoiceFile);


            }
            else if (requestCode == RESULT_VIDEO) {


                toVideoFile = v_path;
                labelVideoFile.setText(DEF_VIDEOFILE + "：" + toVideoFile);


            }
        }
    }

    private void parseResult(String result) {
        String text = "";
        try {
            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.getInt("result") == 0) {
                text = "操作成功";
            } else {
                text = jsonObject.getString("errmsg");
            }
        } catch (Exception e) {
            text = "解析结果失败";
        }
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }
    public static String makeVideoThumbFile(String videoFile)
    {
        File f = new File(videoFile+".jpg");
        if(f.exists())
        {
            return f.toString();
        }
        Bitmap tmp = getVideoThumbnail(videoFile);
        if(tmp!=null)
        {
            File thumbfile = saveImage(tmp,videoFile+".jpg");
            if(thumbfile!=null) {
                return thumbfile.toString();
            }
            else
            {
                return "";
            }
        }
        else
        {
            return "";
        }
    }
    public static File saveImage(Bitmap bmp, String fileName) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        if(fileName==null || fileName.equals("")) {
            fileName = System.currentTimeMillis() + ".jpg";
        }
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
    public static Bitmap getVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();

            if(bitmap.getWidth()>320 || bitmap.getHeight()>200)
            {
                float scale = 1;
                if(bitmap.getWidth()>320)
                {
                    scale = (float)320/(float)bitmap.getWidth();
                }
                else
                {
                    scale = (float)200/(float)bitmap.getHeight();
                }
                Matrix matrix = new Matrix();

                matrix.postScale(scale,scale);

                Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                bitmap = resizeBmp;

            }
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        finally {
            try {
                retriever.release();
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

}
