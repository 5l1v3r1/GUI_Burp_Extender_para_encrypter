//refer "AES Payloads" in burp app store
package burp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.DomainCombiner;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;

import com.alibaba.fastjson.util.Base64;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import burp.CAESOperator_AES_128; //AES�ӽ����㷨��ʵ����
import burp.CUnicode; //unicode�����ʵ����
import burp.IParameter;
import net.miginfocom.swing.MigLayout;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


public class BurpExtender implements IBurpExtender, IHttpListener,ITab,IContextMenuFactory
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������

	private JPanel contentPane;
	private JTextField textFieldDomain;
	private JTable table;
	private JTextField addhere;
	private JTextField txtAESKey;
	private JTextField txtIVString;
	private JCheckBox chckbxProxy;
	private JCheckBox chckbxScanner;
	private JCheckBox chckbxIntruder;
	private JCheckBox chckbxRepeater;
	private JCheckBox decryptResponse;
	private JCheckBox chckbxShowDecryptedOnly;
	private JTextArea textPlain;
	private JTextArea textChiper;
	private JCheckBox checkBoxBase64;
	private JComboBox comboBoxAESMode;
	private JTabbedPane tabbedPane_Center;
    
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
    	stdout.println("Para Encrypter v1.1 by bit4");
    	//System.out.println("test"); ���������burp��
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("Para Encrypter v1.1 by bit4"); //�������
        callbacks.registerHttpListener(this); //���û��ע�ᣬ�����processHttpMessage�����ǲ�����Ч�ġ������������Ӧ���Ĳ�������Ӧ���Ǳ�Ҫ��
        callbacks.registerContextMenuFactory(this);
        addMenuTab();
    }

    @Override
    public void processHttpMessage(int toolFlag,boolean messageIsRequest,IHttpRequestResponse messageInfo)
    {
		List<String> paraWhiteList = new ArrayList<String>(); //�������������������еĲ���ֵ�Ž��м��ܼ���
		paraWhiteList = getParaFromTable();
		
		
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
                
                //�ж�һ�������Ƿ����ļ��ϴ�������
    			boolean isFileUploadRequest =false;
    			for (String header : headers){
    				//stdout.println(header);
    				if (header.toLowerCase().indexOf("content-type")!=-1 && header.toLowerCase().indexOf("boundary")!=-1){//ͨ��httpͷ�е������ж���������Ƿ����ļ��ϴ�������
    					isFileUploadRequest = true;
    				}
    			}
    			
    			if (isFileUploadRequest == false && getHost(analyzeRequest).endsWith(getHostFromUI())){ //���ļ��ϴ������󣬶����еĲ����������ܴ��� ;�������������жϣ�ֻ��ָ���������������������д���
	    			byte[] new_Request = messageInfo.getRequest();
	    			for (IParameter para : paraList){// ѭ����ȡ�������ж����ͣ����м��ܴ�����ٹ����µĲ������ϲ����µ�������С�
	    				if (paraWhiteList.contains(para.getName())){ 
	    					//getTpe()�������жϲ��������Ǹ�λ�õģ�cookie�еĲ����ǲ���Ҫ���м��ܴ���ġ���Ҫ�ų��������еĲ�����
		    				//����Ҫע����ǣ����������͹�6�֣����body�еĲ�����json����xml��ʽ����Ҫ�����жϡ�
	    					String key = para.getName(); //��ȡ����������
		    				String value = para.getValue(); //��ȡ������ֵ
		    				//stdout.println(key+":"+value);
		    				
		    				
		    				try {
		    					
		    					int tabIndex = tabbedPane_Center.getSelectedIndex();
		    					String txtPlain = value;
		    					String encryptedValue = "";
		    					if (tabIndex == 0){
		    						encryptedValue = AESEncrypt(txtPlain);
		    					}else if (tabIndex == 1) {
		    						encryptedValue = Base64Encrypt(txtPlain);
		    					}else if (tabIndex == 2) {
		    						encryptedValue = RSAEncrypt(txtPlain);
		    					}else if (tabIndex == 3 ){
		    						encryptedValue =DESEncrypt(txtPlain);
		    					}

		    					encryptedValue = URLEncoder.encode(encryptedValue); //��Ҫ����URL���룬��������= �������ַ����²����ж��쳣
			    				stdout.println(key+":"+value+":"+encryptedValue); //�����extender��UI���ڣ�������ʹ������һЩ�ж�
			    				//���°��ķ�������
			    				//���²���
			    				IParameter newPara = helpers.buildParameter(key, encryptedValue, para.getType()); //�����µĲ���,���������PARAM_JSON���ͣ���������ǲ����õ�
			    				//IParameter newPara = helpers.buildParameter(key, encryptedValue, PARAM_BODY); //Ҫʹ�����PARAM_BODY �ǲ�����Ҫ��ʵ����IParameter�ࡣ
			    				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ������
			    				// new_Request = helpers.buildHttpMessage(headers, byte_body); //����޸���header�������޸���body��������ͨ��updateParameter��ʹ�����������
							} catch (Exception e) {
								e.printStackTrace();
							}
		    				
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
    			if(this.decryptResponse.isSelected()){
	    			//�����أ���Ӧ��
	    			IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse()); //getResponse��õ����ֽ�����
	    			List<String> header = analyzedResponse.getHeaders();
	    			//short statusCode = analyzedResponse.getStatusCode();
	    			int bodyOffset = analyzedResponse.getBodyOffset();
	    			String resp = new String(messageInfo.getResponse());
                    String body = resp.substring(bodyOffset);
    				try{
    					
    					int tabIndex = tabbedPane_Center.getSelectedIndex();
    					String txtPlain = "";
    					String txtCipher = body;
    					if (tabIndex == 0){
    						txtPlain = AESDecrypt(txtCipher);
    					}else if (tabIndex == 1) {
    						txtPlain = Base64Decrypt(txtCipher);
    					}else if (tabIndex == 2) {
    						txtPlain = RSADecrypt(txtCipher);
    					}else if (tabIndex == 3 ){
    						txtPlain = DESDecrypt(txtCipher);
    					}
    					
    					txtPlain = txtPlain.replace("\"", "\\\"");
	                    String UnicodeBody = (new CUnicode()).unicodeDecode(txtPlain);
	                    String newBody;
	                    if(chckbxShowDecryptedOnly.isSelected()){
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
    
    public void UI() {

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(5, 5));
		
		JPanel panel_North = new JPanel();
		contentPane.add(panel_North, BorderLayout.NORTH);
		panel_North.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_North.add(panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[]{161, 161, 161, 161, 161, 0};
		gbl_panel_3.rowHeights = new int[]{23, 23, 0};
		gbl_panel_3.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel_3.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_3.setLayout(gbl_panel_3);
		
		JLabel enableFor = new JLabel("Enable For :");
		GridBagConstraints gbc_enableFor = new GridBagConstraints();
		gbc_enableFor.fill = GridBagConstraints.BOTH;
		gbc_enableFor.insets = new Insets(0, 0, 5, 5);
		gbc_enableFor.gridx = 0;
		gbc_enableFor.gridy = 0;
		panel_3.add(enableFor, gbc_enableFor);
		
		chckbxProxy = new JCheckBox("Proxy");
		GridBagConstraints gbc_chckbxProxy = new GridBagConstraints();
		gbc_chckbxProxy.fill = GridBagConstraints.BOTH;
		gbc_chckbxProxy.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxProxy.gridx = 1;
		gbc_chckbxProxy.gridy = 0;
		panel_3.add(chckbxProxy, gbc_chckbxProxy);
		
		chckbxScanner = new JCheckBox("Scanner");
		GridBagConstraints gbc_chckbxScanner = new GridBagConstraints();
		gbc_chckbxScanner.fill = GridBagConstraints.BOTH;
		gbc_chckbxScanner.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxScanner.gridx = 2;
		gbc_chckbxScanner.gridy = 0;
		panel_3.add(chckbxScanner, gbc_chckbxScanner);
		
		chckbxIntruder = new JCheckBox("Intruder");
		GridBagConstraints gbc_chckbxIntruder = new GridBagConstraints();
		gbc_chckbxIntruder.fill = GridBagConstraints.BOTH;
		gbc_chckbxIntruder.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxIntruder.gridx = 3;
		gbc_chckbxIntruder.gridy = 0;
		panel_3.add(chckbxIntruder, gbc_chckbxIntruder);
		
		chckbxRepeater = new JCheckBox("Repeater");
		chckbxRepeater.setSelected(true);
		GridBagConstraints gbc_chckbxRepeater = new GridBagConstraints();
		gbc_chckbxRepeater.fill = GridBagConstraints.BOTH;
		gbc_chckbxRepeater.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxRepeater.gridx = 4;
		gbc_chckbxRepeater.gridy = 0;
		panel_3.add(chckbxRepeater, gbc_chckbxRepeater);
		
		JLabel dealResponse = new JLabel("About Response :");
		GridBagConstraints gbc_dealResponse = new GridBagConstraints();
		gbc_dealResponse.fill = GridBagConstraints.BOTH;
		gbc_dealResponse.insets = new Insets(0, 0, 0, 5);
		gbc_dealResponse.gridx = 0;
		gbc_dealResponse.gridy = 1;
		panel_3.add(dealResponse, gbc_dealResponse);
		
		decryptResponse = new JCheckBox("Decrypt Response");
		GridBagConstraints gbc_decryptResponse = new GridBagConstraints();
		gbc_decryptResponse.fill = GridBagConstraints.BOTH;
		gbc_decryptResponse.insets = new Insets(0, 0, 0, 5);
		gbc_decryptResponse.gridx = 1;
		gbc_decryptResponse.gridy = 1;
		panel_3.add(decryptResponse, gbc_decryptResponse);
		
		chckbxShowDecryptedOnly = new JCheckBox("Show Decrypted Content Only");
		GridBagConstraints gbc_chckbxShowDecryptedOnly = new GridBagConstraints();
		gbc_chckbxShowDecryptedOnly.fill = GridBagConstraints.BOTH;
		gbc_chckbxShowDecryptedOnly.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxShowDecryptedOnly.gridx = 2;
		gbc_chckbxShowDecryptedOnly.gridy = 1;
		panel_3.add(chckbxShowDecryptedOnly, gbc_chckbxShowDecryptedOnly);
		
		JPanel panel_South = new JPanel();
		panel_South.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		contentPane.add(panel_South, BorderLayout.SOUTH);
		panel_South.setLayout(new BoxLayout(panel_South, BoxLayout.X_AXIS));
		
		JLabel lblNewLabel = new JLabel("Para Encrypter v1.1 by bit4    https://github.com/bit4woo");
		panel_South.add(lblNewLabel);
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		
		JPanel panel_East = new JPanel();
		contentPane.add(panel_East, BorderLayout.EAST);
		panel_East.setLayout(new BorderLayout(0, 0));
		
		textPlain = new JTextArea(20,20);
		textPlain.setLineWrap(true);
		panel_East.add(textPlain, BorderLayout.WEST);
		
		JPanel panel_9 = new JPanel();
		panel_East.add(panel_9, BorderLayout.CENTER);
		GridBagLayout gbl_panel_9 = new GridBagLayout();
		gbl_panel_9.columnWidths = new int[]{93, 0};
		gbl_panel_9.rowHeights = new int[]{23, 0, 0};
		gbl_panel_9.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_9.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_9.setLayout(gbl_panel_9);
		
		JButton btnNewButton_1 = new JButton("Encrypt ->");
		GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
		gbc_btnNewButton_1.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnNewButton_1.gridx = 0;
		gbc_btnNewButton_1.gridy = 0;
		panel_9.add(btnNewButton_1, gbc_btnNewButton_1);
		btnNewButton_1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				//stdout.println(tabbedPane_Center.getSelectedIndex());
				int tabIndex = tabbedPane_Center.getSelectedIndex();
				String txtPlain = textPlain.getText();
				String txtCipher = "";
				if (tabIndex == 0){
					txtCipher = AESEncrypt(txtPlain);
				}else if (tabIndex == 1) {
					txtCipher = Base64Encrypt(txtPlain);
				}else if (tabIndex == 2) {
					txtCipher = RSAEncrypt(txtPlain);
				}else if (tabIndex == 3 ){
					txtCipher =DESEncrypt(txtPlain);
				}
				textChiper.setText(txtCipher);
			}
		});
		
		JButton btnNewButton_2 = new JButton("<- Decrypt");
		GridBagConstraints gbc_btnNewButton_2 = new GridBagConstraints();
		gbc_btnNewButton_2.gridx = 0;
		gbc_btnNewButton_2.gridy = 1;
		panel_9.add(btnNewButton_2, gbc_btnNewButton_2);
		btnNewButton_2.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//stdout.println(tabbedPane_Center.getSelectedIndex());
				int tabIndex = tabbedPane_Center.getSelectedIndex();
				String txtPlain = "";
				String txtCipher = textChiper.getText();
				if (tabIndex == 0){
					txtPlain = AESDecrypt(txtCipher);
				}else if (tabIndex == 1) {
					txtPlain = Base64Decrypt(txtCipher);
				}else if (tabIndex == 2) {
					txtPlain = RSADecrypt(txtCipher);
				}else if (tabIndex == 3 ){
					txtPlain = DESDecrypt(txtCipher);
				}
				textPlain.setText(txtPlain);
				
			}
		});
		
		textChiper = new JTextArea(20,20);
		textChiper.setLineWrap(true);
		panel_East.add(textChiper, BorderLayout.EAST);
		
		JPanel panel_West = new JPanel();
		panel_West.setPreferredSize(new Dimension(500, 200));
		panel_West.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		contentPane.add(panel_West, BorderLayout.WEST);
		panel_West.setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		panel_West.add(panel, BorderLayout.NORTH);
		panel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JLabel lblDomainName = new JLabel("Domain :");
		panel.add(lblDomainName);
		
		textFieldDomain = new JTextField();
		panel.add(textFieldDomain);
		textFieldDomain.setColumns(10);
		
		JLabel lblParaIncluded = new JLabel("Parameters That Need To Encrypt :");
		panel.add(lblParaIncluded);
		
		table = new JTable();
		table.getTableHeader().setResizingAllowed(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setModel(new DefaultTableModel(
			new Object[][] {
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
			},
			new String[] {
				"key", "value"
			}
		));
		panel_West.add(table, BorderLayout.CENTER);
		
		JPanel panel_1 = new JPanel();
		panel_West.add(panel_1, BorderLayout.EAST);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{69, 0};
		gbl_panel_1.rowHeights = new int[]{23, 0, 0};
		gbl_panel_1.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		JButton btnRemove = new JButton("Remove");
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
            	tableModel.removeRow(table.getSelectedRow());//���һ��ɾ�����У�
			}
		});
		GridBagConstraints gbc_btnRemove = new GridBagConstraints();
		gbc_btnRemove.insets = new Insets(0, 0, 5, 0);
		gbc_btnRemove.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnRemove.gridx = 0;
		gbc_btnRemove.gridy = 0;
		panel_1.add(btnRemove, gbc_btnRemove);
		
		JPanel panel_2 = new JPanel();
		panel_West.add(panel_2, BorderLayout.SOUTH);
		panel_2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		addhere = new JTextField();
		panel_2.add(addhere);
		addhere.setColumns(20);
		
		JButton btnNewButton = new JButton("Add");
		panel_2.add(btnNewButton);
		
		tabbedPane_Center = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane_Center.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		tabbedPane_Center.setSize(new Dimension(20, 20));
		tabbedPane_Center.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		tabbedPane_Center.setMinimumSize(new Dimension(20, 20));
		contentPane.add(tabbedPane_Center, BorderLayout.CENTER);
		
		JPanel panel_4 = new JPanel();
		panel_4.setMinimumSize(new Dimension(20, 20));
		tabbedPane_Center.addTab("AES", null, panel_4, null);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[]{96, 178, 0};
		gbl_panel_4.rowHeights = new int[]{21, 21, 23, 21, 0};
		gbl_panel_4.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_panel_4.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_4.setLayout(gbl_panel_4);
		
		JLabel lblAESkey = new JLabel("AES Key String :");
		GridBagConstraints gbc_lblAESkey = new GridBagConstraints();
		gbc_lblAESkey.anchor = GridBagConstraints.WEST;
		gbc_lblAESkey.insets = new Insets(0, 0, 5, 5);
		gbc_lblAESkey.gridx = 0;
		gbc_lblAESkey.gridy = 0;
		panel_4.add(lblAESkey, gbc_lblAESkey);
		
		txtAESKey = new JTextField();
		txtAESKey.setText("bit4@MZSEC.2016.");
		GridBagConstraints gbc_txtAESKey = new GridBagConstraints();
		gbc_txtAESKey.anchor = GridBagConstraints.NORTH;
		gbc_txtAESKey.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtAESKey.insets = new Insets(0, 0, 5, 0);
		gbc_txtAESKey.gridx = 1;
		gbc_txtAESKey.gridy = 0;
		panel_4.add(txtAESKey, gbc_txtAESKey);
		txtAESKey.setColumns(40);
		
		JLabel label = new JLabel("AES IV String :");
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.anchor = GridBagConstraints.WEST;
		gbc_label.insets = new Insets(0, 0, 5, 5);
		gbc_label.gridx = 0;
		gbc_label.gridy = 1;
		panel_4.add(label, gbc_label);
		
		txtIVString = new JTextField();
		txtIVString.setText("0123456789ABCDEF");
		txtIVString.setColumns(20);
		GridBagConstraints gbc_txtIVString = new GridBagConstraints();
		gbc_txtIVString.anchor = GridBagConstraints.NORTH;
		gbc_txtIVString.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtIVString.insets = new Insets(0, 0, 5, 0);
		gbc_txtIVString.gridx = 1;
		gbc_txtIVString.gridy = 1;
		panel_4.add(txtIVString, gbc_txtIVString);
		
		checkBoxBase64 = new JCheckBox("Base64 Decode/Encode");
		checkBoxBase64.setSelected(true);
		GridBagConstraints gbc_checkBoxBase64 = new GridBagConstraints();
		gbc_checkBoxBase64.anchor = GridBagConstraints.NORTHWEST;
		gbc_checkBoxBase64.insets = new Insets(0, 0, 5, 0);
		gbc_checkBoxBase64.gridx = 1;
		gbc_checkBoxBase64.gridy = 2;
		panel_4.add(checkBoxBase64, gbc_checkBoxBase64);
		
		JLabel label_1 = new JLabel("AES Mode :");
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_label_1.insets = new Insets(0, 0, 0, 5);
		gbc_label_1.gridx = 0;
		gbc_label_1.gridy = 3;
		panel_4.add(label_1, gbc_label_1);
		
		comboBoxAESMode = new JComboBox();
		comboBoxAESMode.setModel(new DefaultComboBoxModel(new String[] {"AES/CBC/PKCS5Padding","AES/ECB/PKCS5Padding","AES/CBC/NoPadding","AES/ECB/NoPadding"}));
		GridBagConstraints gbc_comboBoxAESMode = new GridBagConstraints();
		gbc_comboBoxAESMode.anchor = GridBagConstraints.NORTH;
		gbc_comboBoxAESMode.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxAESMode.gridx = 1;
		gbc_comboBoxAESMode.gridy = 3;
		panel_4.add(comboBoxAESMode, gbc_comboBoxAESMode);

		
		JPanel panel_5 = new JPanel();
		tabbedPane_Center.addTab("Base64", null, panel_5, null);
		
		JLabel lblSwitchToThis = new JLabel("Switch to this tab to use Base64 encryopt and decrypt");
		panel_5.add(lblSwitchToThis);
		
		JPanel panel_6 = new JPanel();
		tabbedPane_Center.addTab("RSA", null, panel_6, null);
		
		JPanel panel_7 = new JPanel();
		tabbedPane_Center.addTab("DES", null, panel_7, null);
	}
    
    
    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          BurpExtender.this.UI();
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
		return this.contentPane;
	}

//	public void getPara(){
//		
////		private JCheckBox chckbxProxy;
////		private JCheckBox chckbxScanner;
////		private JCheckBox chckbxIntruder;
////		private JCheckBox chckbxRepeater;
////		private JCheckBox decryptResponse;
////		private JCheckBox chckbxShowDecryptedOnly;
//		this.AESkey = this.txtAESKey.getText();
//		this.AESIV = this.txtIVString.getText();
//		this.BaseEncode = this.checkBoxBase64.isSelected();
//		this.AESMode = (String)this.comboBoxAESMode.getSelectedItem();
//		this.Plaintext = this.textPlain.getText();
//		this.Chiphertext = this.textChiper.getText();
//	}
	
	public int checkEnabledFor(){
		//get values that should enable this extender for which Component.
		int status = 0;
		if (chckbxIntruder.isSelected()){
			status +=32;
		}
		if(chckbxProxy.isSelected()){
			status += 4;
		}
		if(chckbxRepeater.isSelected()){
			status += 64;
		}
		if(chckbxScanner.isSelected()){
			status += 16;
		}
		return status;
	}


	@Override	
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation)
	{ //��Ҫ��ǩ��ע�ᣡ��callbacks.registerContextMenuFactory(this);
	    IHttpRequestResponse[] messages = invocation.getSelectedMessages();
	    List<JMenuItem> list = new ArrayList<JMenuItem>();
	    if((messages != null) && (messages.length > 0))
	    {
	        //this.callbacks.printOutput("Messages in array: " + messages.length);
	        
	        //final IHttpService service = messages[0].getHttpService();
	    	final byte[] sentRequestBytes = messages[0].getRequest();
	    	IRequestInfo analyzeRequest = helpers.analyzeRequest(sentRequestBytes);
	    	
	        JMenuItem menuItem = new JMenuItem("Send to Para Encrypter");
	        menuItem.addActionListener(new ActionListener()
	        {
	          public void actionPerformed(ActionEvent e)
	          {
	            try
	            {
	            	BurpExtender.this.textFieldDomain.setText(getHost(analyzeRequest));
	            	
	            	DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
	            	tableModel.addRow(new Object[]{"col1","col2","coln"});
	            	
	            	Map<String,String> paraMap = getPara(analyzeRequest);
	            	int i = 0;
	            	for(String key:paraMap.keySet()){
	            		if (i == table.getRowCount()){
	            			tableModel.addRow(new Object[]{"","",""});
	            		}
	            		table.setValueAt(key, i, 0);
	            		table.setValueAt(paraMap.get(key), i, 1);
	            		i += 1;
	            	}
	            }
	            catch (Exception e1)
	            {
	                BurpExtender.this.callbacks.printError(e1.getMessage());
	            }
	          }
	        });
	        list.add(menuItem);
	    }
	    return list;
	}
	public String getHost(IRequestInfo analyzeRequest){
    	List<String> headers = analyzeRequest.getHeaders();
    	
    	String domain = "";
    	for(String item:headers){
    		if (item.toLowerCase().contains("host")){
    			domain = new String(item.substring(6));
    		}
    	}
    	return domain ;
	}
	
	public String getHostFromUI(){
    	String domain = "";
    	domain = textFieldDomain.getText();
    	return domain ;
	}
	
	public Map<String, String> getPara(IRequestInfo analyzeRequest){
    	List<IParameter> paras = analyzeRequest.getParameters();
    	Map<String,String> paraMap = new HashMap<String,String>();
    	for (IParameter para:paras){
    		paraMap.put(para.getName(), para.getValue());
    	}
    	return paraMap ;
	}
	
	public List<String> getParaFromTable(){
    	List<String> whiteParalist = new ArrayList<String>();
    	for (int i=0; i<table.getRowCount();i++){
    		whiteParalist.add(table.getValueAt(i, 0).toString());
    	}
    	return whiteParalist;
	}
	
	
	public String AESEncrypt(String plainText) {
		String AESKey = txtAESKey.getText();
		String AESIV = txtIVString.getText();
		boolean baseEncode = checkBoxBase64.isSelected();
		String AESMode = comboBoxAESMode.getSelectedItem().toString();
		String resultString;
		try {
			resultString = burp.CAES.encrypt(AESKey, AESIV, baseEncode, AESMode, plainText);
			return resultString;
		} catch (Exception e) {
			//e.printStackTrace();
			return e.toString();
		}
		
		
	}
	public String AESDecrypt(String cipherText) {
		String AESKey = txtAESKey.getText();
		String AESIV = txtIVString.getText();
		boolean baseEncode = checkBoxBase64.isSelected();
		String AESMode = comboBoxAESMode.getSelectedItem().toString();
		String resultString;
		try {
			resultString = burp.CAES.decrypt(AESKey, AESIV, baseEncode, AESMode, cipherText);
			return resultString;
		} catch (Exception e) {
			return e.toString();
		}
		
		
	}
	public String Base64Encrypt(String plainText) {
		String resultString =(new BASE64Encoder()).encodeBuffer(plainText.getBytes());
		return resultString;
	}
	public String Base64Decrypt(String cipherText) {
		String resultString;
		try {
			resultString = new String((new BASE64Decoder()).decodeBuffer(cipherText));
			return resultString;
		} catch (IOException e) {
			return e.toString();
		}
	}
	public String RSAEncrypt(String plainText) {
		String resultString = "still not available";
		return resultString;
		
	}
	public String RSADecrypt(String cipherText) {
		String resultString = "still not available";
		return resultString;
		
	}
	public String DESEncrypt(String plainText) {
		String resultString = "still not available";
		return resultString;
		
	}
	public String DESDecrypt(String cipherText) {
		String resultString = "still not available";
		return resultString;
		
	}

}