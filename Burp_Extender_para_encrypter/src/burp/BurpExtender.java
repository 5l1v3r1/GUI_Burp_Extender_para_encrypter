//refer "AES Payloads" in burp app store
package burp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Component;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.GridBagLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.glass.ui.TouchInputSupport;

import burp.CAESOperator_AES_128; //AES�ӽ����㷨��ʵ����
import burp.CUnicode; //unicode�����ʵ����
import burp.IParameter;
import sun.awt.resources.awt;


public class BurpExtender implements IBurpExtender, IHttpListener,ITab
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������
    private JPanel panel;
    public final String TAB_NAME = "AES Config";
    
    private JCheckBox forScanner,forIntruder,forRepeater,forProxy;
    private JCheckBox decrpytResponse,showClearText;
    
    private JLabel hexFormat;
    private JLabel stringFormat;
    private JTextField hexString;
    private JTextField textString;
    private JButton hexButton;
    
    private JTextField parameterAESkey;
    private JTextField parameterAESIV;
    private JLabel lblDescription;
    private JComboBox comboAESMode;
    private JLabel lbl3;
    private JCheckBox chckbxNewCheckBox;
    private JCheckBox chckbxBaseEncode;
    private JPanel panel_1;
    private JPanel panel_0;


    private JButton btnNewButton;
    private JTextArea textAreaPlaintext;
    private JTextArea textAreaCiphertext;
    private JButton btnNewButton_1;
    private JLabel lblPlaintext;
    private JLabel lblCiphertext;
    //public IntruderPayloadProcessor payloadEncryptor;
    //public IntruderPayloadProcessor payloadDecryptor;
    
    private String AESkey; //these parameters are get from GUI use to encrypt or decrypt
    private String AESIV;
    private String AESMode;
    private boolean BaseEncode;
    private String Plaintext;
    private String Chiphertext;
		

    
    // implement IBurpExtender
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	//PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true); ����д���Ƕ��������ʵ����������ı��������µı���������֮ǰclass�е�ȫ�ֱ����ˡ�
    	stdout.println("Para Encrypter v1.0 by bit4");
    	//System.out.println("test"); ���������burp��
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("Para Encrypter v1.0 by bit4"); //�������
        callbacks.registerHttpListener(this); //���û��ע�ᣬ�����processHttpMessage�����ǲ�����Ч�ġ������������Ӧ���Ĳ�������Ӧ���Ǳ�Ҫ��
        addMenuTab();
    }

    @Override
    public void processHttpMessage(int toolFlag,boolean messageIsRequest,IHttpRequestResponse messageInfo)
    {
		List<String> paraWhiteList = new ArrayList<String>(); //�������������������еĲ���ֵ�����м��ܼ���
		paraWhiteList.add("android");
		
		
    	if (toolFlag == (toolFlag&checkEnabledFor())){ //��ͬ��toolflag�����˲�ͬ��burp��� https://portswigger.net/burp/extender/api/constant-values.html#burp.IBurpExtenderCallbacks
    		if (messageIsRequest){ //����������д���
    			
    			//��ȡ���ֲ�������Ϣ��ķ����������£��޷����֣�body��header��paramater
    			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo); //����Ϣ����н��� 
    			//the method of get header
    			List<String> headers = analyzeRequest.getHeaders(); //��ȡhttp����ͷ����Ϣ�����ؿ��Կ�����һ��python�е��б�java���ǽз���ʲô�ģ���ûŪ���
    			//the method of get body
    			int bodyOffset = analyzeRequest.getBodyOffset();
    			byte[] byte_Request = messageInfo.getRequest();
    			String request = new String(byte_Request); //byte[] to String
                String body = request.substring(bodyOffset);
                byte[] byte_body = body.getBytes();  //String to byte[]
    			//the method of get parameter
                List<IParameter> paraList = analyzeRequest.getParameters();//��body��json��ʽ��ʱ���������Ҳ����������ȡ����ֵ�ԣ�ţ��������PARAM_JSON�ȸ�ʽ����ͨ��updateParameter���������¡�
                //�����url�еĲ�����ֵ�� xxx=json��ʽ���ַ��� ������ʽ��ʱ��getParametersӦ�����޷���ȡ����ײ�ļ�ֵ�Եġ�
                //��ȡ���ֲ�������Ϣ�岿�ֵļ��� 
                getPara();//��ȡ��������ϵĸ���ֵ��
                
                //�ж�һ�������Ƿ����ļ��ϴ�������
    			boolean isFileUploadRequest =false;
    			for (String header : headers){
    				//stdout.println(header);
    				if (header.toLowerCase().indexOf("content-type")!=-1 && header.toLowerCase().indexOf("boundary")!=-1){//ͨ��httpͷ�е������ж���������Ƿ����ļ��ϴ�������
    					isFileUploadRequest = true;
    				}
    			}
    			
    			if (isFileUploadRequest == false){ //���ļ��ϴ������󣬶����еĲ����������ܴ���
	    			byte[] new_Request = messageInfo.getRequest();
	    			for (IParameter para : paraList){// ѭ����ȡ�������ж����ͣ����м��ܴ�����ٹ����µĲ������ϲ����µ�������С�
	    				if ((para.getType() == 0 || para.getType() == 1) && !paraWhiteList.contains(para.getName())){ 
	    					//getTpe()�������жϲ��������Ǹ�λ�õģ�cookie�еĲ����ǲ���Ҫ���м��ܴ���ġ���Ҫ�ų��������еĲ�����
		    				//����Ҫע����ǣ����������͹�6�֣����body�еĲ�����json����xml��ʽ����Ҫ�����жϡ�
	    					String key = para.getName(); //��ȡ����������
		    				String value = para.getValue(); //��ȡ������ֵ
		    				//stdout.println(key+":"+value);
		    				
		    				String aesvalue;
		    				try {
								aesvalue = CAES.encrypt(AESkey,AESIV,BaseEncode,AESMode,Plaintext);
								aesvalue = URLEncoder.encode(aesvalue); //��Ҫ����URL���룬��������= �������ַ����²����ж��쳣
			    				stdout.println(key+":"+value+":"+aesvalue); //�����extender��UI���ڣ�������ʹ������һЩ�ж�
			    				//���°��ķ�������
			    				//���²���
			    				IParameter newPara = helpers.buildParameter(key, aesvalue, para.getType()); //�����µĲ���,���������PARAM_JSON���ͣ���������ǲ����õ�
			    				//IParameter newPara = helpers.buildParameter(key, aesvalue, PARAM_BODY); //Ҫʹ�����PARAM_BODY �ǲ�����Ҫ��ʵ����IParameter�ࡣ
			    				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ������
			    				// new_Request = helpers.buildHttpMessage(headers, byte_body); //����޸���header�������޸���body��������ͨ��updateParameter��ʹ�����������
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} //��valueֵ���м���
		    				
	    				}
	    			}
	    			messageInfo.setRequest(new_Request);//���������µ������
    			}
    			/* to verify the updated result
    			for (IParameter para : helpers.analyzeRequest(messageInfo).getParameters()){
    				stdout.println(para.getValue());
    			}
    			*/		
    		}
    		
    		else{
    			if(this.decrpytResponse.isSelected()){
	    			//�����أ���Ӧ��
	    			IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse()); //getResponse��õ����ֽ�����
	    			List<String> header = analyzedResponse.getHeaders();
	    			short statusCode = analyzedResponse.getStatusCode();
	    			int bodyOffset = analyzedResponse.getBodyOffset();
	    			if (statusCode==200){
	    				try{
		    				CAESOperator_AES_128 aes = new CAESOperator_AES_128();
		    				String resp = new String(messageInfo.getResponse());
		                    String body = resp.substring(bodyOffset);
		                    String deBody= CAES.decrypt(AESkey,AESIV,BaseEncode,AESMode,body);
		                    deBody = deBody.replace("\"", "\\\"");
		                    String UnicodeBody = (new CUnicode()).unicodeDecode(deBody);
		                    String newBody;
		                    if(showClearText.isSelected()){
		                    	 newBody = UnicodeBody;
		                    }
		                    else {
		                    	 newBody = body +"\r\n" +UnicodeBody; //���µĽ��ܺ��body�����ɵ�body����
							}
		                    byte[] bodybyte = newBody.getBytes();
		                    //���°��ķ�����buildHttpMessage
		                    messageInfo.setResponse(helpers.buildHttpMessage(header, bodybyte));
	    				}catch(Exception e){
	    					stdout.println(e);
	    				}
	    			}
    			}
    			
    		}	    		
    	}
    		
    }
    
    
    
    public void buildUI(){
       // Create configuration Panel
    	this.panel = new JPanel();
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[] { 139, 400, 0 };
        gbl_panel.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }; //  ���ֶα��ֶ�����С�߶ȵ���д��
        gbl_panel.columnWeights = new double[] { 1.0D, 1.0D, Double.MIN_VALUE }; //     ���ֶα��ֶ�����С��ȵ���д
        gbl_panel.rowWeights = new double[] { 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, Double.MIN_VALUE }; //     ���ֶα��ֶ���Ȩ�ص���д��
        this.panel.setLayout(gbl_panel);
        
        this.lblDescription = new JLabel("<html><b>Para Encrypter v1.0 by bit4</b><br>https://github.com/bit4woo.<br></html>");
        this.lblDescription.setHorizontalAlignment(2);
        this.lblDescription.setVerticalAlignment(1);
        GridBagConstraints gbc_lblDescription = new GridBagConstraints();
        gbc_lblDescription.fill = 2; //��䷽ʽ
        gbc_lblDescription.insets = new Insets(0, 0, 5, 0); //���ü��
        gbc_lblDescription.gridx = 0; //����gridxֵ����ΪGridBagConstriants.RELETIVEʱ������ӵ��������������ǰһ��������Ҳ�
        gbc_lblDescription.gridy = 0; //ͬ����gridy ֵ����ΪGridBagConstraints.RELETIVEʱ������ӵ��������������ǰһ��������·�
        this.panel.add(this.lblDescription, gbc_lblDescription);
        
        this.forScanner = new JCheckBox("Enable For Scanner");
        this.forScanner.setSelected(true);
        this.forIntruder = new JCheckBox("Enable For Intruder");
        this.forIntruder.setSelected(true);
        this.forRepeater = new JCheckBox("Enable For Repeater");
        this.forRepeater.setSelected(true);
        this.forProxy = new JCheckBox("Enable For Proxy");
        this.forProxy.setSelected(true);
        
        this.decrpytResponse = new JCheckBox("Decrypt Response");
        this.decrpytResponse.setSelected(true);
        this.showClearText = new JCheckBox("show decrypted clear content in response only");
        this.showClearText.setSelected(false);
        
        this.panel.add(forScanner);
        this.panel.add(forIntruder);
        this.panel.add(forRepeater);
        this.panel.add(forProxy);
        this.panel.add(decrpytResponse);
        this.panel.add(showClearText);
        
        
        /////ʵ��hex��string��ת��
        this.panel_0 = new JPanel();
        GridBagConstraints gbc_panel_0 = new GridBagConstraints();
        gbc_panel_0.gridwidth = 2;
        gbc_panel_0.insets = new Insets(0, 0, 0, 5);
        gbc_panel_0.fill = 1;
        gbc_panel_0.gridx = 0;
        gbc_panel_0.gridy = 1;
        this.panel.add(this.panel_0, gbc_panel_0);
        GridBagLayout gbl_panel_0 = new GridBagLayout();
//        gbl_panel_0.columnWidths = new int[] { 0, 0, 0, 0 };
//        gbl_panel_0.rowHeights = new int[] { 0, 0, 0, 0 };
//        gbl_panel_0.columnWeights = new double[] { 1.0D, 0.0D, 1.0D, Double.MIN_VALUE };
//        gbl_panel_0.rowWeights = new double[] { 0.0D, 0.0D, 1.0D, Double.MIN_VALUE };
        this.panel_0.setLayout(gbl_panel_0);
        
        this.hexFormat = new JLabel("hexFormat");
        this.hexFormat.setHorizontalAlignment(SwingConstants.LEFT);//���뷽ʽ
        GridBagConstraints gbc_hexFormat = new GridBagConstraints();
        gbc_hexFormat.insets = new Insets(0, 0, 5, 5);
        gbc_hexFormat.gridx = 0;
        gbc_hexFormat.gridy = 0;
        this.panel_0.add(this.hexFormat, gbc_hexFormat);
        
        
        this.stringFormat = new JLabel("stringFormat");
        this.stringFormat.setHorizontalAlignment(4);
        GridBagConstraints gbc_stringFormat = new GridBagConstraints();
        gbc_stringFormat.insets = new Insets(0, 0, 5, 0);
        gbc_stringFormat.gridx = 2;
        gbc_stringFormat.gridy = 0;
        this.panel_0.add(this.stringFormat, gbc_stringFormat);
        
        this.hexString = new JTextField();
        GridBagConstraints gbc_hexString = new GridBagConstraints();
        gbc_hexString.gridheight = 2;
        gbc_hexString.insets = new Insets(0, 0, 0, 5);
        gbc_hexString.fill = 2;
        gbc_hexString.gridx = 0;
        gbc_hexString.gridy = 1;
        this.panel_0.add(this.hexString, gbc_hexString);
        this.hexString.setColumns(40);//�ɹ��������ı���Ŀ��
        
        this.hexButton = new JButton("->");
        this.hexButton.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent arg0)
          {
            try
            {
              getPara();
              BurpExtender.this.textAreaCiphertext.setText(CHexString2String.hexStringToString(BurpExtender.this.hexString.getText()));
            }
            catch (Exception e)
            {
              BurpExtender.this.callbacks.issueAlert(e.toString());
            }
          }
        });
        GridBagConstraints gbc_hexButton = new GridBagConstraints();
        gbc_hexButton.insets = new Insets(0, 0, 5, 5);
        gbc_hexButton.gridx = 1;
        gbc_hexButton.gridy = 1;
        this.panel_0.add(this.hexButton, gbc_hexButton);
        
        this.textString = new JTextField();
        GridBagConstraints gbc_textString = new GridBagConstraints();
        gbc_textString.gridheight = 2;
        gbc_textString.fill = 2;
        gbc_textString.gridx = 2;
        gbc_textString.gridy = 1;
        this.panel_0.add(this.textString, gbc_textString);
        this.textString.setColumns(40);
        
//        this.btnNewButton_1 = new JButton("<-");
//        this.btnNewButton_1.addActionListener(new ActionListener()
//        {
//          public void actionPerformed(ActionEvent arg0)
//          {
//            try
//            {
//              getPara();
//              BurpExtender.this.textAreaPlaintext.setText(CAES.decrypt(AESkey,AESIV,BaseEncode,AESMode,Chiphertext));
//            }
//            catch (Exception e)
//            {
//              BurpExtender.this.callbacks.issueAlert(e.toString());
//            }
//          }
//        });
//        this.btnNewButton_1.setVerticalAlignment(1);
//        GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
//        gbc_btnNewButton_1.anchor = 11;
//        gbc_btnNewButton_1.insets = new Insets(0, 0, 0, 5);
//        gbc_btnNewButton_1.gridx = 1;
//        gbc_btnNewButton_1.gridy = 2;
//        this.panel_0.add(this.btnNewButton_1, gbc_btnNewButton_1);
        //////////ʵ��hex ��string��ת��
        
        
        JLabel lbl1 = new JLabel("AES key String:");
        lbl1.setHorizontalAlignment(4);
        GridBagConstraints gbc_lbl1 = new GridBagConstraints();
        gbc_lbl1.anchor = 13;
        gbc_lbl1.insets = new Insets(0, 0, 5, 5);
        gbc_lbl1.gridx = 0;
        gbc_lbl1.gridy = 2;
        this.panel.add(lbl1, gbc_lbl1);
        
        this.parameterAESkey = new JTextField();
        this.parameterAESkey.setText("bit4@MZSEC.2016.");
        GridBagConstraints gbc_parameterAESkey = new GridBagConstraints();
        gbc_parameterAESkey.insets = new Insets(0, 0, 5, 0);
        gbc_parameterAESkey.fill = 2;
        gbc_parameterAESkey.gridx = 1;
        gbc_parameterAESkey.gridy = 2;
        this.panel.add(this.parameterAESkey, gbc_parameterAESkey);
        this.parameterAESkey.setColumns(10);
        
        JLabel lbl2 = new JLabel("IV String:");
        lbl2.setHorizontalAlignment(4);
        GridBagConstraints gbc_lbl2 = new GridBagConstraints();
        gbc_lbl2.insets = new Insets(0, 0, 5, 5);
        gbc_lbl2.anchor = 13;
        gbc_lbl2.gridx = 0;
        gbc_lbl2.gridy = 3;
        this.panel.add(lbl2, gbc_lbl2);
        
        this.parameterAESIV = new JTextField();
        this.parameterAESIV.setText("0123456789ABCDEF");
        this.parameterAESIV.setColumns(10);
        GridBagConstraints gbc_parameterAESIV = new GridBagConstraints();
        gbc_parameterAESIV.insets = new Insets(0, 0, 5, 0);
        gbc_parameterAESIV.fill = 2;
        gbc_parameterAESIV.gridx = 1;
        gbc_parameterAESIV.gridy = 3;
        this.panel.add(this.parameterAESIV, gbc_parameterAESIV);
        
//        this.chckbxNewCheckBox = new JCheckBox("IV block in Ciphertext (not yet working)");
//        this.chckbxNewCheckBox.setEnabled(false);
//        GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
//        gbc_chckbxNewCheckBox.fill = 2;
//        gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 0);
//        gbc_chckbxNewCheckBox.gridx = 0;
//        gbc_chckbxNewCheckBox.gridy = 4;
//        this.panel.add(this.chckbxNewCheckBox, gbc_chckbxNewCheckBox);
        
        this.chckbxBaseEncode = new JCheckBox("Base 64 Decode/Encode");
        this.chckbxBaseEncode.setSelected(true);
        GridBagConstraints gbc_chckbxBaseEncode = new GridBagConstraints();
        gbc_chckbxBaseEncode.fill = 2;
        gbc_chckbxBaseEncode.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxBaseEncode.gridx = 1;
        gbc_chckbxBaseEncode.gridy = 4;
        this.panel.add(this.chckbxBaseEncode, gbc_chckbxBaseEncode);
        
        this.lbl3 = new JLabel("AES Mode:");
        this.lbl3.setHorizontalAlignment(4);
        GridBagConstraints gbc_lbl3 = new GridBagConstraints();
        gbc_lbl3.insets = new Insets(0, 0, 5, 5);
        gbc_lbl3.anchor = 13;
        gbc_lbl3.gridx = 0;
        gbc_lbl3.gridy = 5;
        this.panel.add(this.lbl3, gbc_lbl3);
        
        this.comboAESMode = new JComboBox();//�����˵�
        this.comboAESMode.addPropertyChangeListener(new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent arg0)
          {
            String cmode = (String)BurpExtender.this.comboAESMode.getSelectedItem();
            if (cmode.contains("CBC")) {
              BurpExtender.this.parameterAESIV.setEditable(true);
            } else {
              BurpExtender.this.parameterAESIV.setEditable(false);
            }
          }
        });
        this.comboAESMode.setModel(new DefaultComboBoxModel(new String[] { "AES/CBC/NoPadding", "AES/CBC/PKCS5Padding", "AES/ECB/NoPadding", "AES/ECB/PKCS5Padding" }));
        this.comboAESMode.setSelectedIndex(1);
        GridBagConstraints gbc_comboAESMode = new GridBagConstraints();
        gbc_comboAESMode.insets = new Insets(0, 0, 5, 0);
        gbc_comboAESMode.fill = 2;
        gbc_comboAESMode.gridx = 1;
        gbc_comboAESMode.gridy = 5;
        this.panel.add(this.comboAESMode, gbc_comboAESMode);
        
        ///�ı������һ������panel
        this.panel_1 = new JPanel();
        GridBagConstraints gbc_panel_1 = new GridBagConstraints();
        gbc_panel_1.gridwidth = 3;
        gbc_panel_1.insets = new Insets(0, 0, 0, 5);
        gbc_panel_1.fill = 1;
        gbc_panel_1.gridx = 0;
        gbc_panel_1.gridy = 6;
        this.panel.add(this.panel_1, gbc_panel_1);
        GridBagLayout gbl_panel_1 = new GridBagLayout();
        gbl_panel_1.columnWidths = new int[] { 0, 0, 0, 0 };
        gbl_panel_1.rowHeights = new int[] { 0, 0, 0, 0 };
        gbl_panel_1.columnWeights = new double[] { 1.0D, 0.0D, 1.0D, Double.MIN_VALUE };
        gbl_panel_1.rowWeights = new double[] { 0.0D, 0.0D, 1.0D, Double.MIN_VALUE };
        this.panel_1.setLayout(gbl_panel_1);
        
        this.lblPlaintext = new JLabel("Plaintext");
        this.lblPlaintext.setHorizontalAlignment(4);
        GridBagConstraints gbc_lblPlaintext = new GridBagConstraints();
        gbc_lblPlaintext.insets = new Insets(0, 0, 5, 5);
        gbc_lblPlaintext.gridx = 0;
        gbc_lblPlaintext.gridy = 0;
        this.panel_1.add(this.lblPlaintext, gbc_lblPlaintext);
        
        this.lblCiphertext = new JLabel("Ciphertext");
        this.lblCiphertext.setHorizontalAlignment(4);
        GridBagConstraints gbc_lblCiphertext = new GridBagConstraints();
        gbc_lblCiphertext.insets = new Insets(0, 0, 5, 0);
        gbc_lblCiphertext.gridx = 2;
        gbc_lblCiphertext.gridy = 0;
        this.panel_1.add(this.lblCiphertext, gbc_lblCiphertext);
        
        this.textAreaPlaintext = new JTextArea();
        this.textAreaPlaintext.setLineWrap(true);
        GridBagConstraints gbc_textAreaPlaintext = new GridBagConstraints();
        gbc_textAreaPlaintext.gridheight = 2;
        gbc_textAreaPlaintext.insets = new Insets(0, 0, 0, 5);
        gbc_textAreaPlaintext.fill = 1;
        gbc_textAreaPlaintext.gridx = 0;
        gbc_textAreaPlaintext.gridy = 1;
        this.panel_1.add(this.textAreaPlaintext, gbc_textAreaPlaintext);
        
        this.btnNewButton = new JButton("Encrypt ->");
        this.btnNewButton.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent arg0)
          {
            try
            {
              getPara();
              BurpExtender.this.textAreaCiphertext.setText(CAES.encrypt(AESkey,AESIV,BaseEncode,AESMode,Plaintext));
            }
            catch (Exception e)
            {
              BurpExtender.this.callbacks.issueAlert(e.toString());
            }
          }
        });
        GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
        gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
        gbc_btnNewButton.gridx = 1;
        gbc_btnNewButton.gridy = 1;
        this.panel_1.add(this.btnNewButton, gbc_btnNewButton);
        
        this.textAreaCiphertext = new JTextArea();
        this.textAreaCiphertext.setLineWrap(true);
        GridBagConstraints gbc_textAreaCiphertext = new GridBagConstraints();
        gbc_textAreaCiphertext.gridheight = 2;
        gbc_textAreaCiphertext.fill = 1;
        gbc_textAreaCiphertext.gridx = 2;
        gbc_textAreaCiphertext.gridy = 1;
        this.panel_1.add(this.textAreaCiphertext, gbc_textAreaCiphertext);
        
        this.btnNewButton_1 = new JButton("<- Decrypt");
        this.btnNewButton_1.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent arg0)
          {
            try
            {
              getPara();
              BurpExtender.this.textAreaPlaintext.setText(CAES.decrypt(AESkey,AESIV,BaseEncode,AESMode,Chiphertext));
            }
            catch (Exception e)
            {
              BurpExtender.this.callbacks.issueAlert(e.toString());
            }
          }
        });
        this.btnNewButton_1.setVerticalAlignment(1);
        GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
        gbc_btnNewButton_1.anchor = 11;
        gbc_btnNewButton_1.insets = new Insets(0, 0, 0, 5);
        gbc_btnNewButton_1.gridx = 1;
        gbc_btnNewButton_1.gridy = 2;
        this.panel_1.add(this.btnNewButton_1, gbc_btnNewButton_1);
    }
    //�ı��򲿷�

    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          BurpExtender.this.buildUI();
          BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this);
        }
      });
    }
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return ("Para Encrypter");
	}

	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return this.panel;
	}

	public void getPara(){
		//get values in AES config panel
		this.AESkey = this.parameterAESkey.getText();
		this.AESIV = this.parameterAESIV.getText();
		this.BaseEncode = this.chckbxBaseEncode.isSelected();
		this.AESMode = (String)this.comboAESMode.getSelectedItem();
		this.Plaintext = this.textAreaPlaintext.getText();
		this.Chiphertext = this.textAreaCiphertext.getText();
	}
	
	public int checkEnabledFor(){
		//get values that should enable this extender for which Component.
		int status = 0;
		if (forIntruder.isSelected()){
			status +=32;
		}
		if(forProxy.isSelected()){
			status += 4;
		}
		if(forRepeater.isSelected()){
			status += 64;
		}
		if(forScanner.isSelected()){
			status += 16;
		}
		return status;
	}
	
}