/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mvjf.OpenMturk;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.mturk.AmazonMTurk;
import com.amazonaws.services.mturk.AmazonMTurkClientBuilder;
import com.amazonaws.services.mturk.model.ApproveAssignmentRequest;
import com.amazonaws.services.mturk.model.Assignment;
import com.amazonaws.services.mturk.model.AssignmentStatus;
import com.amazonaws.services.mturk.model.CreateAdditionalAssignmentsForHITRequest;
import com.amazonaws.services.mturk.model.CreateAdditionalAssignmentsForHITResult;
import com.amazonaws.services.mturk.model.GetAccountBalanceRequest;
import com.amazonaws.services.mturk.model.GetAccountBalanceResult;
import com.amazonaws.services.mturk.model.CreateHITRequest;
import com.amazonaws.services.mturk.model.CreateHITResult;
import com.amazonaws.services.mturk.model.CreateQualificationTypeRequest;
import com.amazonaws.services.mturk.model.CreateQualificationTypeResult;
import com.amazonaws.services.mturk.model.CreateWorkerBlockRequest;
import com.amazonaws.services.mturk.model.DeleteHITRequest;
import com.amazonaws.services.mturk.model.DeleteQualificationTypeRequest;
import com.amazonaws.services.mturk.model.DeleteWorkerBlockRequest;
import com.amazonaws.services.mturk.model.GetHITRequest;
import com.amazonaws.services.mturk.model.GetHITResult;
import com.amazonaws.services.mturk.model.GetQualificationTypeRequest;
import com.amazonaws.services.mturk.model.GetQualificationTypeResult;
import com.amazonaws.services.mturk.model.HIT;
import com.amazonaws.services.mturk.model.ListAssignmentsForHITRequest;
import com.amazonaws.services.mturk.model.ListAssignmentsForHITResult;
import com.amazonaws.services.mturk.model.ListHITsRequest;
import com.amazonaws.services.mturk.model.ListHITsResult;
import com.amazonaws.services.mturk.model.ListQualificationTypesRequest;
import com.amazonaws.services.mturk.model.ListQualificationTypesResult;
import com.amazonaws.services.mturk.model.ListWorkerBlocksRequest;
import com.amazonaws.services.mturk.model.ListWorkerBlocksResult;
import com.amazonaws.services.mturk.model.NotifyWorkersRequest;
import com.amazonaws.services.mturk.model.QualificationRequirement;
import com.amazonaws.services.mturk.model.QualificationType;
import com.amazonaws.services.mturk.model.QualificationTypeStatus;
import com.amazonaws.services.mturk.model.RejectAssignmentRequest;
import com.amazonaws.services.mturk.model.RequestErrorException;
import com.amazonaws.services.mturk.model.ServiceException;
import com.amazonaws.services.mturk.model.SendBonusRequest;
import com.amazonaws.services.mturk.model.UpdateExpirationForHITRequest;
import com.amazonaws.services.mturk.model.UpdateExpirationForHITResult;
import com.amazonaws.services.mturk.model.UpdateQualificationTypeRequest;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author matt
 */
public class OpenMturk extends javax.swing.JFrame {
    AmazonMTurk client;
    List<Assignment> assignments;
    HashMap<String, String> ghitHM;
    HashMap<String, String> gQualificationHM;
    HashMap<String, QualificationRequirement> gQreqHM = new HashMap<>();
    String submitEndpoint = "";
    MessageList gErrorMessageList;

    //common error messages
    String SERVICE_EXCEPTION_MSG = "Amazon Mechanical Turk is temporarily unable to process your request. Wait a minute and try again.";
    String REQUEST_EXCEPTION_MSG = "Your request is invalid.";
    String MISC_EXCEPTION_MSG = "Check your inputs and try again.";
    String NO_HIT_PROVIDED = "You need to provide a HIT ID.";

    String DOLLAR_REGEX = "\\d+(\\.\\d+)?";

    /**
     * Creates new form MAT
     */
    public OpenMturk() {
        initComponents();
        setLocationRelativeTo(null);
        setSelectionModel(lstAssignments);
        setSelectionModel(lstListHITs);
        setSelectionModel(lstBonusHITs);
        setSelectionModel(lstAssignmentHITs);
        setSelectionModel(lstHITsContact);
        setSelectionModel(lstQualificationTypes);
        lstListHITs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstAssignmentHITs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstBonusHITs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstHITsContact.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstQualificationTypes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSignInIndicators(Color.RED);
        checkAndLoadCredentials();
    }
    
    private void setSelectionModel(JList list) {
        ListSelectionModel listSelectionModel = list.getSelectionModel();
        listSelectionModel.addListSelectionListener(new SharedListSelectionHandler(list));
        listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    private void checkAndLoadCredentials() {
        try(BufferedReader br = new BufferedReader(new FileReader("credentials.txt"))) {

            String ACCESS = br.readLine();
            String SECRET = br.readLine();

            txtAccessKey.setText(ACCESS);
            txtSecretKey.setText(SECRET);
        }
        catch(Exception e) {
            //ehh, doesn't matter... if no file or something, just don't do anything
        }
    }
    
    private HashMap<String, String> getSystemQualificationMappings() {
        HashMap<String, String> systemQtypeHM = new HashMap<String, String>();
        String mastersID;
        if (submitEndpoint == "workersandbox.") {
            mastersID = "2ARFPLSP75KLA8M8DH1HTEQVJT3SY6";
        }
        else {
            mastersID = "2ARFPLSP75KLA8M8DH1HTEQVJT3SY6";
        }
        systemQtypeHM.put("Masters", mastersID);
        systemQtypeHM.put("HITs Approved", "00000000000000000040");
        systemQtypeHM.put("Locale", "00000000000000000071");
        systemQtypeHM.put("Adult Workers", "00000000000000000060");
        systemQtypeHM.put("HIT Approval Rate", "000000000000000000L0");
        return systemQtypeHM;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlMain = new javax.swing.JTabbedPane();
        pnlAccount = new javax.swing.JPanel();
        lblAWSCreds = new javax.swing.JLabel();
        txtAccessKey = new javax.swing.JTextField();
        lblAccessKey = new javax.swing.JLabel();
        lblSecretKey = new javax.swing.JLabel();
        txtSecretKey = new javax.swing.JTextField();
        btnValidate = new javax.swing.JButton();
        txtAccountBalance = new javax.swing.JTextField();
        btnGetBalance = new javax.swing.JButton();
        btnOpenWebsite = new javax.swing.JButton();
        btnLoadCredentials = new javax.swing.JButton();
        lblEndpoint = new javax.swing.JLabel();
        CbxEndpoint = new javax.swing.JComboBox<>();
        btnSignout = new javax.swing.JButton();
        pnlSignInAccount = new javax.swing.JPanel();
        lblSignInAccount = new javax.swing.JLabel();
        btnSaveCredentials = new javax.swing.JButton();
        btnDeleteCredentials = new javax.swing.JButton();
        pnlCreateHIT = new javax.swing.JPanel();
        txtHITTitle = new javax.swing.JTextField();
        lblHITTitle = new javax.swing.JLabel();
        txtReward = new javax.swing.JTextField();
        lblReward = new javax.swing.JLabel();
        txtMaxAssignments = new javax.swing.JTextField();
        lblMaxAssignments = new javax.swing.JLabel();
        txtHITLifetime = new javax.swing.JTextField();
        lblHITLifetime = new javax.swing.JLabel();
        sclpnKeywords = new javax.swing.JScrollPane();
        txtKeywords = new javax.swing.JTextArea();
        lblKeywords = new javax.swing.JLabel();
        sclpnDescription = new javax.swing.JScrollPane();
        txtDescription = new javax.swing.JTextArea();
        lblDescription = new javax.swing.JLabel();
        txtAutoApprvlDelay = new javax.swing.JTextField();
        lblAutoApprveDelay = new javax.swing.JLabel();
        lblAssignDuration = new javax.swing.JLabel();
        txtAssignDuration = new javax.swing.JTextField();
        btnCreateHIT = new javax.swing.JButton();
        lblExternalURL = new javax.swing.JLabel();
        txtExternalURL = new javax.swing.JTextField();
        lblQualifications = new javax.swing.JLabel();
        pnlSignInCreateHIT = new javax.swing.JPanel();
        lblSignInCreateHIT = new javax.swing.JLabel();
        lblRewardUnits = new javax.swing.JLabel();
        lblAssignmentDurationUnit = new javax.swing.JLabel();
        lblAutoApprovalUnit = new javax.swing.JLabel();
        lblHITLifetimeUnit = new javax.swing.JLabel();
        lblNumofParticipantsUnit = new javax.swing.JLabel();
        lyrpnMicrobatchSelect = new javax.swing.JLayeredPane();
        chkMicroBatch = new javax.swing.JCheckBox();
        lblMicroBatch = new javax.swing.JLabel();
        lblEstimatedCost = new javax.swing.JLabel();
        sclHITQualificationRequirements = new javax.swing.JScrollPane();
        lstHITQualificationRequirements = new javax.swing.JList<>();
        btnModifyQualificationRequirements = new javax.swing.JButton();
        pnlHITDetail = new javax.swing.JPanel();
        btnDeleteHIT = new javax.swing.JButton();
        btnListHITs = new javax.swing.JButton();
        sclListHITs = new javax.swing.JScrollPane();
        lstListHITs = new javax.swing.JList<>();
        lblHITTitleDetail = new javax.swing.JLabel();
        lblHITDescriptionDetail = new javax.swing.JLabel();
        lblAssignmentDurationDetail = new javax.swing.JLabel();
        lblAutoApprovalDelayDetail = new javax.swing.JLabel();
        lblCreationTimeDetail = new javax.swing.JLabel();
        lblHITExpirationTimeDetail = new javax.swing.JLabel();
        lblHITGroupIDDetail = new javax.swing.JLabel();
        lblHITLayoutIDDetail = new javax.swing.JLabel();
        lblHITReviewStatusDetail = new javax.swing.JLabel();
        lblHITStatusDetail = new javax.swing.JLabel();
        lblHITTypeIDDetail = new javax.swing.JLabel();
        lblKeywordsDetail = new javax.swing.JLabel();
        lblMaxAssignmentsDetail = new javax.swing.JLabel();
        lblRewardDetail = new javax.swing.JLabel();
        lblQualificationsDetail = new javax.swing.JLabel();
        btnUpdateHIT = new javax.swing.JButton();
        txtMaxAssignmentsDetail = new javax.swing.JTextField();
        spnMonthDetail = new javax.swing.JSpinner();
        spnDayDetail = new javax.swing.JSpinner();
        spnYearDetail = new javax.swing.JSpinner();
        spnSecondDetail = new javax.swing.JSpinner();
        spnHourDetail = new javax.swing.JSpinner();
        spnMinuteDetail = new javax.swing.JSpinner();
        btnExpireHIT = new javax.swing.JButton();
        lblAddAssignments = new javax.swing.JLabel();
        pnlSignInHITDetail = new javax.swing.JPanel();
        lblSignInHITDetail = new javax.swing.JLabel();
        lblHITsUpdate = new javax.swing.JLabel();
        pnlQualifications = new javax.swing.JPanel();
        lblSignInQualification = new javax.swing.JLabel();
        pnlSignInQualification = new javax.swing.JPanel();
        sclQualificationTypes = new javax.swing.JScrollPane();
        lstQualificationTypes = new javax.swing.JList<>();
        lblCreateQualification = new javax.swing.JLabel();
        chkAutoGranted = new javax.swing.JCheckBox();
        txtQualificationName = new javax.swing.JTextField();
        lblName = new javax.swing.JLabel();
        sclQualificationDescription = new javax.swing.JScrollPane();
        txtQualficationDescription = new javax.swing.JTextArea();
        lblQualificationDesc = new javax.swing.JLabel();
        txtQualificationKeywords = new javax.swing.JTextField();
        lblQualificationKeywords = new javax.swing.JLabel();
        btnDeleteQualification = new javax.swing.JButton();
        btnCreateQualification = new javax.swing.JButton();
        btnListQualification = new javax.swing.JButton();
        btnUpdateQualification = new javax.swing.JButton();
        pnlAppRejAssignments = new javax.swing.JPanel();
        sclAssignments = new javax.swing.JScrollPane();
        lstAssignments = new javax.swing.JList<>();
        btnApproveAll = new javax.swing.JButton();
        btnApproveSelected = new javax.swing.JButton();
        btnRejectSelected = new javax.swing.JButton();
        lblAssignmentDetails = new javax.swing.JLabel();
        lblAssignmentID = new javax.swing.JLabel();
        lblWorkerID = new javax.swing.JLabel();
        lblHITIDDetail = new javax.swing.JLabel();
        lblAssignmentStatus = new javax.swing.JLabel();
        lblAutoApprovalTime = new javax.swing.JLabel();
        lblAcceptTime = new javax.swing.JLabel();
        lblSubmitTime = new javax.swing.JLabel();
        lblApprovalTime = new javax.swing.JLabel();
        lblRejectionTime = new javax.swing.JLabel();
        lblDeadline = new javax.swing.JLabel();
        lblRequesterFeedback = new javax.swing.JLabel();
        sclRequesterFeedback = new javax.swing.JScrollPane();
        txtRequesterFeedback = new javax.swing.JTextArea();
        sclAssignmentHITs = new javax.swing.JScrollPane();
        lstAssignmentHITs = new javax.swing.JList<>();
        btnListHITsAssignment = new javax.swing.JButton();
        lblAssignmentHITs = new javax.swing.JLabel();
        pnlSignInAssignment = new javax.swing.JPanel();
        lblSignInAssignment = new javax.swing.JLabel();
        btnUpdateAssignments = new javax.swing.JButton();
        lblAssignments = new javax.swing.JLabel();
        pnlBonuses = new javax.swing.JPanel();
        lblWorkerIDs = new javax.swing.JLabel();
        lblAssignmentIDs = new javax.swing.JLabel();
        lblBonusAmount = new javax.swing.JLabel();
        txtBonusAmount = new javax.swing.JTextField();
        lblReason = new javax.swing.JLabel();
        sclReason = new javax.swing.JScrollPane();
        txtReason = new javax.swing.JTextArea();
        btnSendBonus = new javax.swing.JButton();
        lblBonusHITs = new javax.swing.JLabel();
        btnListBonusHITs = new javax.swing.JButton();
        sclBonusHITs = new javax.swing.JScrollPane();
        lstBonusHITs = new javax.swing.JList<>();
        btnSelectAllWorkersBonus = new javax.swing.JButton();
        sclWorkerIDsBonus = new javax.swing.JScrollPane();
        lstWorkerIDsBonus = new javax.swing.JList<>();
        sclAssignmentIDsBonus = new javax.swing.JScrollPane();
        lstAssignmentIDsBonus = new javax.swing.JList<>();
        pnlSignInBonuses = new javax.swing.JPanel();
        lblSignInWorkerBonus = new javax.swing.JLabel();
        btnDownloadCSVBonus = new javax.swing.JButton();
        btnUploadCSVBonus = new javax.swing.JButton();
        pnlContactWorkers = new javax.swing.JPanel();
        txtContactSubject = new javax.swing.JTextField();
        lblSubject = new javax.swing.JLabel();
        lblMessage = new javax.swing.JLabel();
        sclMessage = new javax.swing.JScrollPane();
        txtContactMessage = new javax.swing.JTextArea();
        btnContactMessage = new javax.swing.JButton();
        lblRecipients = new javax.swing.JLabel();
        sclRecipients = new javax.swing.JScrollPane();
        txtRecipients = new javax.swing.JTextArea();
        sclHITsContact = new javax.swing.JScrollPane();
        lstHITsContact = new javax.swing.JList<>();
        btnListHITsContact = new javax.swing.JButton();
        lblHITsContact = new javax.swing.JLabel();
        pnlSignInContactWorkers = new javax.swing.JPanel();
        lblSignInContactWorkers = new javax.swing.JLabel();
        pnlBlkUblkWorkers = new javax.swing.JPanel();
        sclBlockList = new javax.swing.JScrollPane();
        lstBlockedWorkers = new javax.swing.JList<>();
        lblBlockedWorkers = new javax.swing.JLabel();
        lblBlkWorkerID = new javax.swing.JLabel();
        btnBlockWorker = new javax.swing.JButton();
        lblBlkReason = new javax.swing.JLabel();
        sclBlkReason = new javax.swing.JScrollPane();
        txtBlkReason = new javax.swing.JTextArea();
        btnListBlockedWorkers = new javax.swing.JButton();
        cmbxAction = new javax.swing.JComboBox<>();
        sclBlockWorkers = new javax.swing.JScrollPane();
        txtBlockWorkerIDs = new javax.swing.JTextArea();
        pnlSignInBlockUnBlockWorkers = new javax.swing.JPanel();
        lblSignInBlockUnBlockWorkers = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("OpenMturk");
        getContentPane().setLayout(new java.awt.FlowLayout());

        pnlMain.setEnabled(false);
        pnlMain.setPreferredSize(new java.awt.Dimension(1429, 800));

        lblAWSCreds.setText("AWS Credentials");

        lblAccessKey.setText("Access Key");

        lblSecretKey.setText("Secret Key");

        btnValidate.setText("Validate Credentials");
        btnValidate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnValidateMouseClicked(evt);
            }
        });

        txtAccountBalance.setEditable(false);
        txtAccountBalance.setEnabled(false);

        btnGetBalance.setText("Check Account Balance");
        btnGetBalance.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnGetBalanceMouseClicked(evt);
            }
        });

        btnOpenWebsite.setText("Open Requester Website");
        btnOpenWebsite.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnOpenWebsiteMouseClicked(evt);
            }
        });

        btnLoadCredentials.setText("Load Credentials CSV");
        btnLoadCredentials.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnLoadCredentialsMouseClicked(evt);
            }
        });

        lblEndpoint.setText("MTurk EndPoint:");

        CbxEndpoint.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Production (requester.mturk.com)", "Sandbox (requestersandbox.mturk.com)" }));

        btnSignout.setText("Sign Out");
        btnSignout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSignoutMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnlSignInAccountLayout = new javax.swing.GroupLayout(pnlSignInAccount);
        pnlSignInAccount.setLayout(pnlSignInAccountLayout);
        pnlSignInAccountLayout.setHorizontalGroup(
            pnlSignInAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInAccountLayout.setVerticalGroup(
            pnlSignInAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        lblSignInAccount.setText("Logged Out");

        btnSaveCredentials.setText("Save Credentials");
        btnSaveCredentials.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSaveCredentialsMouseClicked(evt);
            }
        });

        btnDeleteCredentials.setText("Delete Credentials");
        btnDeleteCredentials.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnDeleteCredentialsMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnlAccountLayout = new javax.swing.GroupLayout(pnlAccount);
        pnlAccount.setLayout(pnlAccountLayout);
        pnlAccountLayout.setHorizontalGroup(
            pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlAccountLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lblSignInAccount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInAccount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(pnlAccountLayout.createSequentialGroup()
                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlAccountLayout.createSequentialGroup()
                        .addGap(416, 416, 416)
                        .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlAccountLayout.createSequentialGroup()
                                .addGap(205, 205, 205)
                                .addComponent(lblAWSCreds))
                            .addGroup(pnlAccountLayout.createSequentialGroup()
                                .addGap(56, 56, 56)
                                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtSecretKey, javax.swing.GroupLayout.PREFERRED_SIZE, 377, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtAccessKey, javax.swing.GroupLayout.PREFERRED_SIZE, 377, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblSecretKey)
                                    .addComponent(lblAccessKey, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(pnlAccountLayout.createSequentialGroup()
                                .addComponent(btnValidate)
                                .addGap(18, 18, 18)
                                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnSaveCredentials, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnLoadCredentials))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnSignout, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                                    .addComponent(btnDeleteCredentials, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGroup(pnlAccountLayout.createSequentialGroup()
                        .addGap(350, 350, 350)
                        .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnOpenWebsite, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblEndpoint, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CbxEndpoint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(175, 175, 175)
                        .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtAccountBalance, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnGetBalance))))
                .addContainerGap(390, Short.MAX_VALUE))
        );
        pnlAccountLayout.setVerticalGroup(
            pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlAccountLayout.createSequentialGroup()
                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblSignInAccount, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                    .addComponent(pnlSignInAccount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(48, 48, 48)
                .addComponent(lblAWSCreds)
                .addGap(13, 13, 13)
                .addComponent(lblAccessKey)
                .addGap(24, 24, 24)
                .addComponent(txtAccessKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblSecretKey)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtSecretKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnValidate)
                    .addComponent(btnLoadCredentials)
                    .addComponent(btnSignout))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSaveCredentials)
                    .addComponent(btnDeleteCredentials))
                .addGap(111, 111, 111)
                .addComponent(lblEndpoint)
                .addGap(18, 18, 18)
                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CbxEndpoint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAccountBalance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlAccountLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOpenWebsite)
                    .addComponent(btnGetBalance))
                .addContainerGap(249, Short.MAX_VALUE))
        );

        btnOpenWebsite.getAccessibleContext().setAccessibleName("Reload");

        pnlMain.addTab("Account Management", pnlAccount);

        pnlCreateHIT.setPreferredSize(new java.awt.Dimension(1158, 970));

        lblHITTitle.setText("HIT Title:");
        lblHITTitle.setToolTipText("The title of the HIT");

        txtReward.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtRewardFocusLost(evt);
            }
        });

        lblReward.setText("Reward:");
        lblReward.setToolTipText("The amount of money the Requester will pay a Worker for successfully completing the HIT in USD (e.g. 12.75 is $12.75 US)");

        txtMaxAssignments.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtMaxAssignmentsFocusLost(evt);
            }
        });

        lblMaxAssignments.setText("Number of Participants:");
        lblMaxAssignments.setToolTipText("The number of times the HIT can be accepted and completed before the HIT becomes unavailable");

        lblHITLifetime.setText("HIT Lifetime:");
        lblHITLifetime.setToolTipText("The amount of time, in seconds, after which the HIT is no longer available for users to accept");

        txtKeywords.setColumns(20);
        txtKeywords.setLineWrap(true);
        txtKeywords.setRows(5);
        sclpnKeywords.setViewportView(txtKeywords);

        lblKeywords.setText("Keywords:");
        lblKeywords.setToolTipText("One or more words or phrases that describe the HIT, separated by commas");

        txtDescription.setColumns(20);
        txtDescription.setLineWrap(true);
        txtDescription.setRows(5);
        txtDescription.setWrapStyleWord(true);
        sclpnDescription.setViewportView(txtDescription);

        lblDescription.setText("Description:");
        lblDescription.setToolTipText("A general description of the HIT");

        lblAutoApprveDelay.setText("Auto Approval Delay:");
        lblAutoApprveDelay.setToolTipText("The amount of time, in seconds, after the Worker submits an assignment for the HIT that the results are automatically approved by Amazon Mechanical Turk");

        lblAssignDuration.setText("Assignment Duration:");
        lblAssignDuration.setToolTipText("The length of time, in seconds, that a Worker has to complete the HIT after accepting it");

        btnCreateHIT.setText("Create HIT");
        btnCreateHIT.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnCreateHITMouseClicked(evt);
            }
        });

        lblExternalURL.setText("Study/Survey URL:");
        lblExternalURL.setToolTipText("The URL of the survey to be completed");

        txtExternalURL.setPreferredSize(new java.awt.Dimension(4, 25));

        lblQualifications.setText("Qualifications:");
        lblQualifications.setToolTipText("Conditions that a Worker's Qualifications must meet in order to accept the HIT");

        javax.swing.GroupLayout pnlSignInCreateHITLayout = new javax.swing.GroupLayout(pnlSignInCreateHIT);
        pnlSignInCreateHIT.setLayout(pnlSignInCreateHITLayout);
        pnlSignInCreateHITLayout.setHorizontalGroup(
            pnlSignInCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInCreateHITLayout.setVerticalGroup(
            pnlSignInCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        lblSignInCreateHIT.setText("Logged Out");

        lblRewardUnits.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        lblRewardUnits.setText("dollars (eg. 12.50)");

        lblAssignmentDurationUnit.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        lblAssignmentDurationUnit.setText("minutes");

        lblAutoApprovalUnit.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        lblAutoApprovalUnit.setText("hours");

        lblHITLifetimeUnit.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        lblHITLifetimeUnit.setText("hours");

        lblNumofParticipantsUnit.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        lblNumofParticipantsUnit.setText("participants");

        lyrpnMicrobatchSelect.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        chkMicroBatch.setText("True");
        chkMicroBatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkMicroBatchActionPerformed(evt);
            }
        });

        lblMicroBatch.setText("MicroBatch:");
        lblMicroBatch.setToolTipText("MicroBatch enables \"batching\" for HITs with over 9 assignments, this will separate assignments into HITs of 9 each to save on mturk costs");

        lyrpnMicrobatchSelect.setLayer(chkMicroBatch, javax.swing.JLayeredPane.DEFAULT_LAYER);
        lyrpnMicrobatchSelect.setLayer(lblMicroBatch, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout lyrpnMicrobatchSelectLayout = new javax.swing.GroupLayout(lyrpnMicrobatchSelect);
        lyrpnMicrobatchSelect.setLayout(lyrpnMicrobatchSelectLayout);
        lyrpnMicrobatchSelectLayout.setHorizontalGroup(
            lyrpnMicrobatchSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lyrpnMicrobatchSelectLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblMicroBatch)
                .addContainerGap())
            .addGroup(lyrpnMicrobatchSelectLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(chkMicroBatch)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        lyrpnMicrobatchSelectLayout.setVerticalGroup(
            lyrpnMicrobatchSelectLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lyrpnMicrobatchSelectLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblMicroBatch)
                .addGap(18, 18, 18)
                .addComponent(chkMicroBatch)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        lblEstimatedCost.setText("Estimated Cost:");

        lstHITQualificationRequirements.setEnabled(false);
        lstHITQualificationRequirements.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                lstHITQualificationRequirementsPropertyChange(evt);
            }
        });
        sclHITQualificationRequirements.setViewportView(lstHITQualificationRequirements);

        btnModifyQualificationRequirements.setText("Modify Qualification Requirements");
        btnModifyQualificationRequirements.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnModifyQualificationRequirementsMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnlCreateHITLayout = new javax.swing.GroupLayout(pnlCreateHIT);
        pnlCreateHIT.setLayout(pnlCreateHITLayout);
        pnlCreateHITLayout.setHorizontalGroup(
            pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCreateHITLayout.createSequentialGroup()
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sclpnDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 452, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblDescription)
                            .addComponent(sclpnKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblKeywords)
                            .addComponent(lblMaxAssignments)
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(txtMaxAssignments, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(lblNumofParticipantsUnit))
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(txtReward, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(lblRewardUnits))
                            .addComponent(lblAssignDuration)
                            .addComponent(lblReward)
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(txtAssignDuration, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(lblAssignmentDurationUnit))
                            .addComponent(lblAutoApprveDelay)
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(txtAutoApprvlDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(1, 1, 1)
                                .addComponent(lblAutoApprovalUnit))
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(txtHITLifetime, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(lblHITLifetimeUnit))
                            .addComponent(lblHITLifetime))
                        .addGap(98, 98, 98)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(lyrpnMicrobatchSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(145, 145, 145)
                                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblEstimatedCost)
                                    .addComponent(btnCreateHIT, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(sclHITQualificationRequirements, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(btnModifyQualificationRequirements, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE))
                            .addComponent(lblQualifications, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(pnlCreateHITLayout.createSequentialGroup()
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblHITTitle)
                            .addComponent(txtHITTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 452, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(98, 98, 98)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtExternalURL, javax.swing.GroupLayout.PREFERRED_SIZE, 359, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblExternalURL, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlCreateHITLayout.createSequentialGroup()
                .addContainerGap(1295, Short.MAX_VALUE)
                .addComponent(lblSignInCreateHIT)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInCreateHIT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlCreateHITLayout.setVerticalGroup(
            pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlSignInCreateHIT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSignInCreateHIT, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(92, 92, 92)
                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblHITTitle)
                    .addComponent(lblExternalURL))
                .addGap(10, 10, 10)
                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCreateHITLayout.createSequentialGroup()
                        .addComponent(txtHITTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblDescription)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sclpnDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblKeywords)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sclpnKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(pnlCreateHITLayout.createSequentialGroup()
                        .addComponent(txtExternalURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblQualifications)
                        .addGap(18, 18, 18)
                        .addComponent(sclHITQualificationRequirements, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnModifyQualificationRequirements)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCreateHITLayout.createSequentialGroup()
                        .addComponent(lblMaxAssignments)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtMaxAssignments, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblNumofParticipantsUnit))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblReward)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtReward, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRewardUnits))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAssignDuration)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtAssignDuration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblAssignmentDurationUnit))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAutoApprveDelay)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtAutoApprvlDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblAutoApprovalUnit))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblHITLifetime))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlCreateHITLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lyrpnMicrobatchSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(pnlCreateHITLayout.createSequentialGroup()
                                .addComponent(lblEstimatedCost)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnCreateHIT)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlCreateHITLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtHITLifetime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHITLifetimeUnit))
                .addGap(148, 148, 148))
        );

        pnlMain.addTab("Create HIT", pnlCreateHIT);

        btnDeleteHIT.setText("Delete HIT");
        btnDeleteHIT.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnDeleteHITMouseClicked(evt);
            }
        });

        btnListHITs.setText("List HITs");
        btnListHITs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListHITsMouseClicked(evt);
            }
        });

        lstListHITs.setName("lstListHITs"); // NOI18N
        sclListHITs.setViewportView(lstListHITs);

        lblHITTitleDetail.setText("Hit Title:");

        lblHITDescriptionDetail.setText("HIT Description:");

        lblAssignmentDurationDetail.setText("Assignment Duration:");

        lblAutoApprovalDelayDetail.setText("Auto Approval Delay:");

        lblCreationTimeDetail.setText("HIT Creation Time:");

        lblHITExpirationTimeDetail.setText("HIT Expiration Date (ss/mm/hh/dd/mm/yyyy):");
        lblHITExpirationTimeDetail.setToolTipText("Update the expiration date of the HIT");

        lblHITGroupIDDetail.setText("HIT Group ID:");

        lblHITLayoutIDDetail.setText("HIT Layout ID:");

        lblHITReviewStatusDetail.setText("HIT Review Status:");

        lblHITStatusDetail.setText("HIT Status:");

        lblHITTypeIDDetail.setText("Hit Type ID:");

        lblKeywordsDetail.setText("Keywords:");

        lblMaxAssignmentsDetail.setText("Max Assignments:");

        lblRewardDetail.setText("Reward:");

        lblQualificationsDetail.setText("Qualifications:");

        btnUpdateHIT.setText("Update HIT");
        btnUpdateHIT.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnUpdateHITMouseClicked(evt);
            }
        });

        btnExpireHIT.setText("Expire HIT");
        btnExpireHIT.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnExpireHITMouseClicked(evt);
            }
        });

        lblAddAssignments.setText("Add Assignments:");
        lblAddAssignments.setToolTipText("Update the number of assignments of a given HIT");

        javax.swing.GroupLayout pnlSignInHITDetailLayout = new javax.swing.GroupLayout(pnlSignInHITDetail);
        pnlSignInHITDetail.setLayout(pnlSignInHITDetailLayout);
        pnlSignInHITDetailLayout.setHorizontalGroup(
            pnlSignInHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInHITDetailLayout.setVerticalGroup(
            pnlSignInHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        lblSignInHITDetail.setText("Logged Out");

        lblHITsUpdate.setText("HITs:");

        javax.swing.GroupLayout pnlHITDetailLayout = new javax.swing.GroupLayout(pnlHITDetail);
        pnlHITDetail.setLayout(pnlHITDetailLayout);
        pnlHITDetailLayout.setHorizontalGroup(
            pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHITDetailLayout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblHITReviewStatusDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAssignmentDurationDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAutoApprovalDelayDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHITLayoutIDDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHITStatusDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHITTypeIDDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblKeywordsDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMaxAssignmentsDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHITGroupIDDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCreationTimeDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlHITDetailLayout.createSequentialGroup()
                        .addComponent(lblHITExpirationTimeDetail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(spnSecondDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(spnMinuteDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(spnHourDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(spnDayDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(spnMonthDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(spnYearDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblHITDescriptionDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRewardDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblQualificationsDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlHITDetailLayout.createSequentialGroup()
                        .addGap(364, 364, 364)
                        .addComponent(btnExpireHIT, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnUpdateHIT, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnDeleteHIT, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlHITDetailLayout.createSequentialGroup()
                        .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlHITDetailLayout.createSequentialGroup()
                                .addComponent(lblAddAssignments, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtMaxAssignmentsDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lblHITTitleDetail, javax.swing.GroupLayout.PREFERRED_SIZE, 1006, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sclListHITs, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblHITsUpdate)
                            .addComponent(btnListHITs, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlHITDetailLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblSignInHITDetail)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInHITDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlHITDetailLayout.setVerticalGroup(
            pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHITDetailLayout.createSequentialGroup()
                .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlHITDetailLayout.createSequentialGroup()
                        .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pnlSignInHITDetail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblSignInHITDetail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(39, 39, 39)
                        .addComponent(lblHITTitleDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblAssignmentDurationDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblAutoApprovalDelayDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblCreationTimeDetail)
                        .addGap(18, 18, 18)
                        .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblHITExpirationTimeDetail)
                            .addComponent(spnMonthDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spnDayDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spnYearDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spnSecondDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spnHourDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spnMinuteDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(lblHITGroupIDDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblHITLayoutIDDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblHITReviewStatusDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblHITStatusDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblHITTypeIDDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblKeywordsDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblMaxAssignmentsDetail)
                        .addGap(18, 18, 18)
                        .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblAddAssignments)
                            .addComponent(txtMaxAssignmentsDetail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(lblHITDescriptionDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblQualificationsDetail)
                        .addGap(18, 18, 18)
                        .addComponent(lblRewardDetail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnlHITDetailLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(lblHITsUpdate)
                        .addGap(18, 18, 18)
                        .addComponent(sclListHITs, javax.swing.GroupLayout.PREFERRED_SIZE, 374, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnListHITs)
                        .addGap(77, 77, 77)))
                .addGroup(pnlHITDetailLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnExpireHIT)
                    .addComponent(btnUpdateHIT)
                    .addComponent(btnDeleteHIT))
                .addGap(352, 352, 352))
        );

        pnlMain.addTab("Update HIT", pnlHITDetail);

        lblSignInQualification.setText("Logged Out");

        javax.swing.GroupLayout pnlSignInQualificationLayout = new javax.swing.GroupLayout(pnlSignInQualification);
        pnlSignInQualification.setLayout(pnlSignInQualificationLayout);
        pnlSignInQualificationLayout.setHorizontalGroup(
            pnlSignInQualificationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInQualificationLayout.setVerticalGroup(
            pnlSignInQualificationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        sclQualificationTypes.setMaximumSize(new java.awt.Dimension(259, 260));
        sclQualificationTypes.setMinimumSize(new java.awt.Dimension(259, 260));

        lstQualificationTypes.setName("lstQualificationTypes"); // NOI18N
        sclQualificationTypes.setViewportView(lstQualificationTypes);

        lblCreateQualification.setText("Create Qualification Type:");

        chkAutoGranted.setSelected(true);
        chkAutoGranted.setText("Auto Granted");

        lblName.setText("<html>Name:<br>\n(non updatable)</html>");

        txtQualficationDescription.setColumns(20);
        txtQualficationDescription.setRows(5);
        sclQualificationDescription.setViewportView(txtQualficationDescription);

        lblQualificationDesc.setText("Description:");

        lblQualificationKeywords.setText("<html>Keywords:<br>\n(comma separated, <br>\nnon updatable)</html>");

        btnDeleteQualification.setText("Delete Selected Qualification");
        btnDeleteQualification.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnDeleteQualificationMouseClicked(evt);
            }
        });

        btnCreateQualification.setText("Create Qualification");
        btnCreateQualification.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnCreateQualificationMouseClicked(evt);
            }
        });

        btnListQualification.setText("List Qualifications");
        btnListQualification.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListQualificationMouseClicked(evt);
            }
        });

        btnUpdateQualification.setText("Update Selected Qualification");
        btnUpdateQualification.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnUpdateQualificationMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnlQualificationsLayout = new javax.swing.GroupLayout(pnlQualifications);
        pnlQualifications.setLayout(pnlQualificationsLayout);
        pnlQualificationsLayout.setHorizontalGroup(
            pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlQualificationsLayout.createSequentialGroup()
                .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlQualificationsLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnDeleteQualification, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlQualificationsLayout.createSequentialGroup()
                        .addGap(157, 157, 157)
                        .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlQualificationsLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(lblCreateQualification))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlQualificationsLayout.createSequentialGroup()
                                .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblQualificationDesc)
                                    .addComponent(lblQualificationKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(pnlQualificationsLayout.createSequentialGroup()
                                        .addComponent(btnCreateQualification)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 471, Short.MAX_VALUE)
                                        .addComponent(btnUpdateQualification, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlQualificationsLayout.createSequentialGroup()
                                        .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(sclQualificationDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtQualificationKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(chkAutoGranted)
                                            .addComponent(txtQualificationName, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(sclQualificationTypes, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlQualificationsLayout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(btnListQualification, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                .addGap(207, 207, 207))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlQualificationsLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lblSignInQualification)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInQualification, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlQualificationsLayout.setVerticalGroup(
            pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlQualificationsLayout.createSequentialGroup()
                .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlSignInQualification, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSignInQualification, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlQualificationsLayout.createSequentialGroup()
                        .addGap(134, 134, 134)
                        .addComponent(lblCreateQualification)
                        .addGap(32, 32, 32)
                        .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtQualificationName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(29, 29, 29)
                        .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblQualificationDesc)
                            .addComponent(sclQualificationDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtQualificationKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblQualificationKeywords, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(chkAutoGranted))
                    .addGroup(pnlQualificationsLayout.createSequentialGroup()
                        .addGap(146, 146, 146)
                        .addComponent(sclQualificationTypes, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(btnListQualification)
                .addGap(15, 15, 15)
                .addGroup(pnlQualificationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCreateQualification)
                    .addComponent(btnUpdateQualification))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnDeleteQualification)
                .addContainerGap(190, Short.MAX_VALUE))
        );

        pnlMain.addTab("Qualification Management", pnlQualifications);

        lstAssignments.setName("lstAssignments"); // NOI18N
        sclAssignments.setViewportView(lstAssignments);

        btnApproveAll.setText("Approve All");
        btnApproveAll.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnApproveAllMouseClicked(evt);
            }
        });

        btnApproveSelected.setText("Approve Selected");
        btnApproveSelected.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnApproveSelectedMouseClicked(evt);
            }
        });

        btnRejectSelected.setText("Reject Selected");
        btnRejectSelected.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnRejectSelectedMouseClicked(evt);
            }
        });

        lblAssignmentDetails.setText("Assignment Details:");

        lblAssignmentID.setText("Assignment ID:");

        lblWorkerID.setText("Worker ID:");

        lblHITIDDetail.setText("HIT ID:");

        lblAssignmentStatus.setText("Assignment Status:");

        lblAutoApprovalTime.setText("Auto Approval Time:");

        lblAcceptTime.setText("Accept Time:");

        lblSubmitTime.setText("Submit Time:");

        lblApprovalTime.setText("Approval TIme:");

        lblRejectionTime.setText("Rejection Time:");

        lblDeadline.setText("Deadline:");

        lblRequesterFeedback.setText("Requester Feedback:");
        lblRequesterFeedback.setToolTipText("Submit some feedback to the worker upon approval or rejection of their work");

        txtRequesterFeedback.setColumns(20);
        txtRequesterFeedback.setLineWrap(true);
        txtRequesterFeedback.setRows(5);
        txtRequesterFeedback.setWrapStyleWord(true);
        sclRequesterFeedback.setViewportView(txtRequesterFeedback);

        lstAssignmentHITs.setName("lstAssignmentHITs"); // NOI18N
        sclAssignmentHITs.setViewportView(lstAssignmentHITs);

        btnListHITsAssignment.setText("List HITs");
        btnListHITsAssignment.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListHITsAssignmentMouseClicked(evt);
            }
        });

        lblAssignmentHITs.setText("HITs:");
        lblAssignmentHITs.setToolTipText("A list of HITs created by the requester");

        javax.swing.GroupLayout pnlSignInAssignmentLayout = new javax.swing.GroupLayout(pnlSignInAssignment);
        pnlSignInAssignment.setLayout(pnlSignInAssignmentLayout);
        pnlSignInAssignmentLayout.setHorizontalGroup(
            pnlSignInAssignmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInAssignmentLayout.setVerticalGroup(
            pnlSignInAssignmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        lblSignInAssignment.setText("Logged Out");

        btnUpdateAssignments.setText("Update Assignments");
        btnUpdateAssignments.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnUpdateAssignmentsMouseClicked(evt);
            }
        });

        lblAssignments.setText("Pending Assignments:");

        javax.swing.GroupLayout pnlAppRejAssignmentsLayout = new javax.swing.GroupLayout(pnlAppRejAssignments);
        pnlAppRejAssignments.setLayout(pnlAppRejAssignmentsLayout);
        pnlAppRejAssignmentsLayout.setHorizontalGroup(
            pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlAppRejAssignmentsLayout.createSequentialGroup()
                .addGap(55, 55, 55)
                .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblAssignmentDetails, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDeadline, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblRejectionTime, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .addComponent(lblApprovalTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSubmitTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAssignmentID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblWorkerID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblHITIDDetail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAssignmentStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAutoApprovalTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblAcceptTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblRequesterFeedback, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sclRequesterFeedback))
                .addGap(243, 243, 243)
                .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(sclAssignmentHITs)
                        .addComponent(btnListHITsAssignment, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblAssignmentHITs))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(btnUpdateAssignments, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnApproveAll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlAppRejAssignmentsLayout.createSequentialGroup()
                            .addComponent(btnApproveSelected)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(btnRejectSelected, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(sclAssignments, javax.swing.GroupLayout.Alignment.LEADING))
                    .addComponent(lblAssignments))
                .addGap(57, 57, 57))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlAppRejAssignmentsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblSignInAssignment)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInAssignment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlAppRejAssignmentsLayout.setVerticalGroup(
            pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlAppRejAssignmentsLayout.createSequentialGroup()
                .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlAppRejAssignmentsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblAssignmentDetails)
                        .addGap(28, 28, 28)
                        .addComponent(lblAssignmentID)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblWorkerID)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblHITIDDetail)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAssignmentStatus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAutoApprovalTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAcceptTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblSubmitTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblApprovalTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblRejectionTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblDeadline)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblRequesterFeedback)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sclRequesterFeedback, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlAppRejAssignmentsLayout.createSequentialGroup()
                        .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pnlSignInAssignment, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblSignInAssignment, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(11, 11, 11)
                        .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblAssignmentHITs)
                            .addComponent(lblAssignments))
                        .addGap(18, 18, 18)
                        .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sclAssignments)
                            .addComponent(sclAssignmentHITs, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnListHITsAssignment)
                            .addComponent(btnUpdateAssignments))))
                .addGap(9, 9, 9)
                .addGroup(pnlAppRejAssignmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnApproveSelected)
                    .addComponent(btnRejectSelected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnApproveAll)
                .addContainerGap(149, Short.MAX_VALUE))
        );

        pnlMain.addTab("Assignment Management", pnlAppRejAssignments);

        lblWorkerIDs.setText("Worker IDs:");
        lblWorkerIDs.setToolTipText("The IDs of the Workers being paid the bonus");

        lblAssignmentIDs.setText("Assignment IDs:");
        lblAssignmentIDs.setToolTipText("The ID of the assignment for which the bonuses are paid");

        lblBonusAmount.setText("Bonus Amount: (Dollars, eg. 2.50)");
        lblBonusAmount.setToolTipText("The bonus to be paid to the workers, in USD");

        lblReason.setText("Reason:");
        lblReason.setToolTipText("A message that explains the reason for the bonus payment. The Worker receiving the bonus can see this message");

        txtReason.setColumns(20);
        txtReason.setLineWrap(true);
        txtReason.setRows(5);
        txtReason.setWrapStyleWord(true);
        sclReason.setViewportView(txtReason);

        btnSendBonus.setText("Send Bonus");
        btnSendBonus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSendBonusMouseClicked(evt);
            }
        });

        lblBonusHITs.setText("Surveys:");
        lblBonusHITs.setToolTipText("The HITs created by the requester");

        btnListBonusHITs.setText("List mturk Surveys");
        btnListBonusHITs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListBonusHITsMouseClicked(evt);
            }
        });

        lstBonusHITs.setModel(new DefaultListModel());
        lstBonusHITs.setName("lstBonusHITs"); // NOI18N
        sclBonusHITs.setViewportView(lstBonusHITs);

        btnSelectAllWorkersBonus.setText("Select All Workers");
        btnSelectAllWorkersBonus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSelectAllWorkersBonusMouseClicked(evt);
            }
        });

        lstWorkerIDsBonus.setModel(new DefaultListModel());
        sclWorkerIDsBonus.setViewportView(lstWorkerIDsBonus);

        lstAssignmentIDsBonus.setModel(new DefaultListModel());
        lstAssignmentIDsBonus.setFocusable(false);
        sclAssignmentIDsBonus.setViewportView(lstAssignmentIDsBonus);

        javax.swing.GroupLayout pnlSignInBonusesLayout = new javax.swing.GroupLayout(pnlSignInBonuses);
        pnlSignInBonuses.setLayout(pnlSignInBonusesLayout);
        pnlSignInBonusesLayout.setHorizontalGroup(
            pnlSignInBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInBonusesLayout.setVerticalGroup(
            pnlSignInBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        lblSignInWorkerBonus.setText("Logged Out");

        btnDownloadCSVBonus.setText("Download Worker CSV");
        btnDownloadCSVBonus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnDownloadCSVBonusMouseClicked(evt);
            }
        });

        btnUploadCSVBonus.setText("Upload Worker CSV");
        btnUploadCSVBonus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnUploadCSVBonusMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnlBonusesLayout = new javax.swing.GroupLayout(pnlBonuses);
        pnlBonuses.setLayout(pnlBonusesLayout);
        pnlBonusesLayout.setHorizontalGroup(
            pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBonusesLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblSignInWorkerBonus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInBonuses, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(pnlBonusesLayout.createSequentialGroup()
                .addGap(96, 96, 96)
                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlBonusesLayout.createSequentialGroup()
                        .addComponent(lblBonusHITs)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnlBonusesLayout.createSequentialGroup()
                        .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(btnUploadCSVBonus, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnListBonusHITs, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sclBonusHITs, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 43, Short.MAX_VALUE)
                        .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlBonusesLayout.createSequentialGroup()
                                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sclWorkerIDsBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblWorkerIDs))
                                .addGap(41, 41, 41)
                                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblAssignmentIDs)
                                    .addGroup(pnlBonusesLayout.createSequentialGroup()
                                        .addComponent(sclAssignmentIDsBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(104, 104, 104)
                                        .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtBonusAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(sclReason, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(btnSendBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lblReason)
                                            .addComponent(lblBonusAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(btnDownloadCSVBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addComponent(btnSelectAllWorkersBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(145, 145, 145))))
        );
        pnlBonusesLayout.setVerticalGroup(
            pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBonusesLayout.createSequentialGroup()
                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlSignInBonuses, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSignInWorkerBonus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(43, 43, 43)
                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblAssignmentIDs)
                        .addComponent(lblBonusHITs))
                    .addComponent(lblWorkerIDs, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlBonusesLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sclBonusHITs, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
                            .addComponent(sclWorkerIDsBonus)
                            .addComponent(sclAssignmentIDsBonus)))
                    .addGroup(pnlBonusesLayout.createSequentialGroup()
                        .addGap(94, 94, 94)
                        .addComponent(lblBonusAmount)
                        .addGap(18, 18, 18)
                        .addComponent(txtBonusAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblReason)
                        .addGap(18, 18, 18)
                        .addComponent(sclReason, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSendBonus)
                        .addGap(18, 18, 18)
                        .addComponent(btnDownloadCSVBonus)))
                .addGap(18, 18, 18)
                .addGroup(pnlBonusesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnListBonusHITs)
                    .addComponent(btnSelectAllWorkersBonus))
                .addGap(18, 18, 18)
                .addComponent(btnUploadCSVBonus)
                .addContainerGap(83, Short.MAX_VALUE))
        );

        pnlMain.addTab("Worker Bonuses", pnlBonuses);

        lblSubject.setText("Subject:");
        lblSubject.setToolTipText("The subject line of the email message to send. Can include up to 200 characters");

        lblMessage.setText("Message:");
        lblMessage.setToolTipText("The text of the email message to send. Can include up to 4,096 characters");

        txtContactMessage.setColumns(20);
        txtContactMessage.setLineWrap(true);
        txtContactMessage.setRows(5);
        txtContactMessage.setWrapStyleWord(true);
        sclMessage.setViewportView(txtContactMessage);

        btnContactMessage.setText("Send Message");
        btnContactMessage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnContactMessageMouseClicked(evt);
            }
        });

        lblRecipients.setText("Worker IDs:");
        lblRecipients.setToolTipText("An array of WorkerIds to notify, each WorkerId needs to be separated by a carriage return");

        txtRecipients.setColumns(20);
        txtRecipients.setRows(5);
        sclRecipients.setViewportView(txtRecipients);

        lstHITsContact.setName("lstHITsContact"); // NOI18N
        sclHITsContact.setViewportView(lstHITsContact);

        btnListHITsContact.setText("List HITs");
        btnListHITsContact.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListHITsContactMouseClicked(evt);
            }
        });

        lblHITsContact.setText("HITs:");
        lblHITsContact.setToolTipText("The HITs created by the requester");

        javax.swing.GroupLayout pnlSignInContactWorkersLayout = new javax.swing.GroupLayout(pnlSignInContactWorkers);
        pnlSignInContactWorkers.setLayout(pnlSignInContactWorkersLayout);
        pnlSignInContactWorkersLayout.setHorizontalGroup(
            pnlSignInContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInContactWorkersLayout.setVerticalGroup(
            pnlSignInContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        lblSignInContactWorkers.setText("Logged Out");

        javax.swing.GroupLayout pnlContactWorkersLayout = new javax.swing.GroupLayout(pnlContactWorkers);
        pnlContactWorkers.setLayout(pnlContactWorkersLayout);
        pnlContactWorkersLayout.setHorizontalGroup(
            pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlContactWorkersLayout.createSequentialGroup()
                .addGap(71, 71, 71)
                .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sclMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtContactSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 249, Short.MAX_VALUE)
                .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnContactMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 473, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlContactWorkersLayout.createSequentialGroup()
                        .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(btnListHITsContact, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(sclHITsContact, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE))
                            .addComponent(lblHITsContact))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sclRecipients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRecipients, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(131, 131, 131))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlContactWorkersLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lblSignInContactWorkers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInContactWorkers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlContactWorkersLayout.setVerticalGroup(
            pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlContactWorkersLayout.createSequentialGroup()
                .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlSignInContactWorkers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSignInContactWorkers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4)
                .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSubject)
                    .addComponent(lblRecipients)
                    .addComponent(lblHITsContact))
                .addGap(18, 18, 18)
                .addGroup(pnlContactWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sclHITsContact)
                    .addGroup(pnlContactWorkersLayout.createSequentialGroup()
                        .addComponent(txtContactSubject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(24, 24, 24)
                        .addComponent(lblMessage)
                        .addGap(18, 18, 18)
                        .addComponent(sclMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(sclRecipients))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnListHITsContact)
                .addGap(76, 76, 76)
                .addComponent(btnContactMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(224, Short.MAX_VALUE))
        );

        pnlMain.addTab("Contact Workers", pnlContactWorkers);

        sclBlockList.setViewportView(lstBlockedWorkers);

        lblBlockedWorkers.setText("Blocked Workers");
        lblBlockedWorkers.setToolTipText("A list of all workers blocked by the requester");

        lblBlkWorkerID.setText("Worker IDs:");
        lblBlkWorkerID.setToolTipText("The IDs of the Workers to block or unblock, each WorkerID must be separated by a carriage return  ");

        btnBlockWorker.setText("Block Workers");
        btnBlockWorker.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnBlockWorkerMouseClicked(evt);
            }
        });

        lblBlkReason.setText("Reason:");
        lblBlkReason.setToolTipText("A message that explains the reason for blocking the Worker. The Worker does not see this message");

        txtBlkReason.setColumns(20);
        txtBlkReason.setLineWrap(true);
        txtBlkReason.setRows(5);
        txtBlkReason.setWrapStyleWord(true);
        sclBlkReason.setViewportView(txtBlkReason);

        btnListBlockedWorkers.setText("Populate Blocked Workers");
        btnListBlockedWorkers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnListBlockedWorkersMouseClicked(evt);
            }
        });

        cmbxAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Block Workers", "UnBlock Workers" }));
        cmbxAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbxActionActionPerformed(evt);
            }
        });

        txtBlockWorkerIDs.setColumns(20);
        txtBlockWorkerIDs.setRows(5);
        sclBlockWorkers.setViewportView(txtBlockWorkerIDs);

        javax.swing.GroupLayout pnlSignInBlockUnBlockWorkersLayout = new javax.swing.GroupLayout(pnlSignInBlockUnBlockWorkers);
        pnlSignInBlockUnBlockWorkers.setLayout(pnlSignInBlockUnBlockWorkersLayout);
        pnlSignInBlockUnBlockWorkersLayout.setHorizontalGroup(
            pnlSignInBlockUnBlockWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );
        pnlSignInBlockUnBlockWorkersLayout.setVerticalGroup(
            pnlSignInBlockUnBlockWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        lblSignInBlockUnBlockWorkers.setText("Logged Out");

        javax.swing.GroupLayout pnlBlkUblkWorkersLayout = new javax.swing.GroupLayout(pnlBlkUblkWorkers);
        pnlBlkUblkWorkers.setLayout(pnlBlkUblkWorkersLayout);
        pnlBlkUblkWorkersLayout.setHorizontalGroup(
            pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBlkUblkWorkersLayout.createSequentialGroup()
                .addGap(138, 138, 138)
                .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblBlkWorkerID, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sclBlockWorkers, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 208, Short.MAX_VALUE)
                .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblBlkReason, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sclBlkReason, javax.swing.GroupLayout.PREFERRED_SIZE, 323, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbxAction, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBlockWorker, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(119, 119, 119)
                .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnListBlockedWorkers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sclBlockList, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblBlockedWorkers))
                .addGap(127, 127, 127))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBlkUblkWorkersLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lblSignInBlockUnBlockWorkers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlSignInBlockUnBlockWorkers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlBlkUblkWorkersLayout.setVerticalGroup(
            pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBlkUblkWorkersLayout.createSequentialGroup()
                .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlSignInBlockUnBlockWorkers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSignInBlockUnBlockWorkers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBlkReason)
                    .addComponent(lblBlkWorkerID)
                    .addComponent(lblBlockedWorkers))
                .addGap(18, 18, 18)
                .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlBlkUblkWorkersLayout.createSequentialGroup()
                        .addComponent(sclBlockList, javax.swing.GroupLayout.PREFERRED_SIZE, 327, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnListBlockedWorkers)
                        .addContainerGap(335, Short.MAX_VALUE))
                    .addGroup(pnlBlkUblkWorkersLayout.createSequentialGroup()
                        .addGroup(pnlBlkUblkWorkersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlBlkUblkWorkersLayout.createSequentialGroup()
                                .addComponent(sclBlkReason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(32, 32, 32)
                                .addComponent(cmbxAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnBlockWorker))
                            .addComponent(sclBlockWorkers, javax.swing.GroupLayout.PREFERRED_SIZE, 445, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        pnlMain.addTab("Block/UnBlock Workers", pnlBlkUblkWorkers);

        getContentPane().add(pnlMain);
        pnlMain.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnValidateMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnValidateMouseClicked
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(txtAccessKey.getText().trim(), txtSecretKey.getText().trim());
        AwsClientBuilder.EndpointConfiguration endpoint;
        if (CbxEndpoint.getModel().getSelectedItem() == "Sandbox (requestersandbox.mturk.com)") {
            endpoint = new AwsClientBuilder.EndpointConfiguration("https://mturk-requester-sandbox.us-east-1.amazonaws.com", "us-east-1");
            submitEndpoint = "workersandbox.";
            client = AmazonMTurkClientBuilder.standard().withEndpointConfiguration(endpoint).withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        }
        else if (CbxEndpoint.getModel().getSelectedItem() == "Production (requester.mturk.com)") {
            client = AmazonMTurkClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        }

        List<String[]> msgList = new ArrayList<String[]>();
        String status;
        int messageType;
        
        if (validCredentials()) {
            status = "Valid credentials; logged in";
            messageType = JOptionPane.INFORMATION_MESSAGE;
            pnlMain.setEnabled(true);
            setSignInIndicators(Color.GREEN);
        }
        else {
            status = "Invalid credentials";
            messageType = JOptionPane.ERROR_MESSAGE;
        }
        String[] msg = {
            "AWS credentials",
            status
        };

        msgList.add(msg);

        showMessage(msgList, messageType);
    }//GEN-LAST:event_btnValidateMouseClicked

    
    private void saveCredentials() {
        try {
            PrintWriter writer = new PrintWriter("credentials.txt", "UTF-8");
            writer.println(txtAccessKey.getText().trim());
            writer.println(txtSecretKey.getText().trim());
            writer.close();
        }
        catch (Exception e) {
            System.out.println("Error writing credentials to file.");
        }
    }
    
    private boolean validCredentials() {
        try {
            GetAccountBalanceRequest request = new GetAccountBalanceRequest();
            client.getAccountBalance(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void btnGetBalanceMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnGetBalanceMouseClicked

        try {
            GetAccountBalanceRequest request = new GetAccountBalanceRequest();
            GetAccountBalanceResult result = client.getAccountBalance(request);

            txtAccountBalance.setText(result.getAvailableBalance());
        }
        catch (ServiceException e) {
            showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (RequestErrorException e) {
            showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            showSingleMessage("Exception", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnGetBalanceMouseClicked
    
    private void btnDeleteHITMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteHITMouseClicked
        try {
            String deleteHITID = ghitHM.get(lstListHITs.getSelectedValue());
            int confirm = JOptionPane.showConfirmDialog(pnlHITDetail, "Are you sure you want to delete this HIT? HITID: " + deleteHITID + "\nNote that if this HIT is part of a microbatch your study will end", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                DeleteHITRequest request = new DeleteHITRequest();
                request.withHITId(deleteHITID);
                client.deleteHIT(request);
                deleteHBQualifications(deleteHITID);
                btnListHITsMouseClicked(evt);
                showSingleMessage("Status", "HIT: " + deleteHITID + " Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        catch (ServiceException e) {
            showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (RequestErrorException e) {
            showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDeleteHITMouseClicked
    
    private void deleteHBQualifications(String hitID) {
        GetHITRequest hitRequest = new GetHITRequest();
        hitRequest.setHITId(hitID);
        GetHITResult hitResult = client.getHIT(hitRequest);
        HIT hit = hitResult.getHIT();
        List<QualificationRequirement> qreqs = hit.getQualificationRequirements();
        for (int i = 0; i < qreqs.size(); i++) {
            String qtypeid = qreqs.get(i).getQualificationTypeId();
            GetQualificationTypeRequest qtypeReq = new GetQualificationTypeRequest();
            qtypeReq.setQualificationTypeId(qtypeid);
            GetQualificationTypeResult qtypeRes = client.getQualificationType(qtypeReq);
            if (qtypeRes.getQualificationType().getName().contains(hit.getTitle() + "-batch")) {
                DeleteQualificationTypeRequest delReq = new DeleteQualificationTypeRequest();
                delReq.setQualificationTypeId(qtypeid);
                try {
                    client.deleteQualificationType(delReq);
                } catch (Exception e) {
                }
            }
        }
    }
    
    private void btnContactMessageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnContactMessageMouseClicked
        NotifyWorkersRequest request = new NotifyWorkersRequest();

        String contactSubject = txtContactSubject.getText().trim();
        String contactMessage = txtContactMessage.getText().trim();
        String recipients = txtRecipients.getText().trim();

        List<String[]> errorList = new ArrayList<String[]>();

        if (contactSubject.length() <= 0) {
            String[] error = {
                "Contact Subject",
                "Need a subject!"
            };
            errorList.add(error);
        }
        if (contactMessage.length() <= 0) {
            String[] error = {
                "Contact Message",
                "Need a message"
            };
            errorList.add(error);
        }
        if (recipients.length() <= 0) {
            String[] error = {
                "Recipients",
                "Need recipient(s)"
            };
            errorList.add(error);
        }

        if (errorList.size() > 0) {
            showMessage(errorList, JOptionPane.WARNING_MESSAGE);
        }
        else {
            request.setSubject(contactSubject);
            request.setMessageText(contactMessage);

            String workerArray[] = txtRecipients.getText().split("\\r?\\n");
            ArrayList<String>workerList = new ArrayList<>(Arrays.asList(workerArray));
            request.setWorkerIds(workerList);

            try {
                client.notifyWorkers(request);
                txtRecipients.setText("");
                showSingleMessage("Status", "Workers Notified Successfully", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (ServiceException e) {
                showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
            }
            catch (RequestErrorException e) {
                showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
            }
            catch (NullPointerException e) {
                showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
            }

        }

    }//GEN-LAST:event_btnContactMessageMouseClicked

    private void btnSendBonusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSendBonusMouseClicked
        String bonus = txtBonusAmount.getText().trim();
        String reason = txtReason.getText().trim();
        List<String[]> errorList = new ArrayList<String[]>();
        
        if (bonus.length() <= 0) {
            String[] error = {
                "Bonus",
                "'You need a bonus!"
            };
            errorList.add(error);
        }
        
        if (reason.length() <= 0) {
            String[] error = {
                "Reason",
                "'You need a reason!"
            };
            errorList.add(error);
        }
        
        if (lstWorkerIDsBonus.getSelectedIndices().length <= 0) {
            String[] error = {
                "Workers",
                "'You need to select workers!"
            };
            errorList.add(error);
        }
        
        if (errorList.size() > 0) {
            showMessage(errorList, JOptionPane.WARNING_MESSAGE);
            return;
        }
        else {
            for (int i = 0; i < lstWorkerIDsBonus.getSelectedIndices().length; i++) {
                String workerId = lstWorkerIDsBonus.getSelectedValuesList().get(i);
                int assignmentIndex = lstWorkerIDsBonus.getSelectedIndices()[i];
                String assignmentId = lstAssignmentIDsBonus.getModel().getElementAt(assignmentIndex);
                SendBonusRequest request = new SendBonusRequest();
                request.setWorkerId(workerId);
                request.setAssignmentId(assignmentId);
                request.setReason(reason);
                request.setBonusAmount(bonus);
                client.sendBonus(request);
            }
            showSingleMessage("Status", "Bonuses Sent", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_btnSendBonusMouseClicked

    private void btnApproveAllMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveAllMouseClicked
        for (int i = 0; i < lstAssignments.getModel().getSize(); i++) {
            ApproveAssignmentRequest request = new ApproveAssignmentRequest().withAssignmentId(lstAssignments.getModel().getElementAt(i));

            try {
                client.approveAssignment(request);
            }
            catch (ServiceException e) {
                showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (RequestErrorException e) {
                showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (NullPointerException e) {
                showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (Exception e) {
                showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        txtRequesterFeedback.setText("");
        btnListHITsAssignmentMouseClicked(evt);
        setAssignments();
        showSingleMessage("Status", "Assignments Approved", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnApproveAllMouseClicked

    private void btnApproveSelectedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveSelectedMouseClicked
        List<String> selectedAssignments = lstAssignments.getSelectedValuesList();
        for (int i = 0; i < selectedAssignments.size(); i++) {
            ApproveAssignmentRequest request = new ApproveAssignmentRequest().withAssignmentId(selectedAssignments.get(i));
            if (!txtRequesterFeedback.getText().isEmpty()) {
                request.setRequesterFeedback(txtRequesterFeedback.getText());
            }

            try {
                client.approveAssignment(request);
            }
            catch (ServiceException e) {
                showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (RequestErrorException e) {
                showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (NullPointerException e) {
                showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (Exception e) {
                showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        txtRequesterFeedback.setText("");
        btnListHITsAssignmentMouseClicked(evt);
        setAssignments();
        showSingleMessage("Status", "Assignments Approved", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnApproveSelectedMouseClicked

    private void btnRejectSelectedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectSelectedMouseClicked
        List<String> selectedAssignments = lstAssignments.getSelectedValuesList();
        for (int i = 0; i < selectedAssignments.size(); i++) {
            RejectAssignmentRequest request = new RejectAssignmentRequest().withAssignmentId(selectedAssignments.get(i));
            if (!txtRequesterFeedback.getText().isEmpty()) {
                request.setRequesterFeedback(txtRequesterFeedback.getText());
            }

            try {
                client.rejectAssignment(request);
            }
            catch (ServiceException e) {
                showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (RequestErrorException e) {
                showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (NullPointerException e) {
                showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
            catch (Exception e) {
                showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        txtRequesterFeedback.setText("");
        btnListHITsAssignmentMouseClicked(evt);
        setAssignments();
        showSingleMessage("Status", "Assignments Rejected", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnRejectSelectedMouseClicked

    private void btnBlockWorkerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnBlockWorkerMouseClicked
        String workers = txtBlockWorkerIDs.getText().trim();

        String workerArray[] = workers.split("\\r?\\n");

        if (workers.length() <= 0) {
            showSingleMessage("Worker ID", "Need to enter a worker ID", JOptionPane.WARNING_MESSAGE);
        }
        else if (txtBlkReason.getText().trim().isEmpty()) {
            showSingleMessage("Reason", "Need to enter a reason", JOptionPane.WARNING_MESSAGE);
        }
        else {
            if (cmbxAction.getSelectedItem() == "Block Workers") {
                blockWorkers(workerArray);
            }
            else if (cmbxAction.getSelectedItem() == "UnBlock Workers") {
                unblockWorkers(workerArray);
            }
        }
    }//GEN-LAST:event_btnBlockWorkerMouseClicked

    private void blockWorkers(String[] workers) {
        for (int i = 0; i < workers.length; i++) {
                CreateWorkerBlockRequest request = new CreateWorkerBlockRequest();
                request.setWorkerId(workers[i]);
                request.setReason(txtBlkReason.getText());

                try {
                    client.createWorkerBlock(request);
                }
                catch (ServiceException e) {
                    showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                catch (RequestErrorException e) {
                    showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                catch (NullPointerException e) {
                    showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                catch (Exception e) {
                    showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
        }
        showSingleMessage("Status", "Workers Blocked", JOptionPane.INFORMATION_MESSAGE);
        txtBlkReason.setText("");
        txtBlockWorkerIDs.setText("");
    }

    private void unblockWorkers(String[] workers) {
        for (int i = 0; i < workers.length; i++) {
                DeleteWorkerBlockRequest request = new DeleteWorkerBlockRequest();
                request.setWorkerId(workers[i]);
                request.setReason(txtBlkReason.getText());

                try {
                    client.deleteWorkerBlock(request);
                }
                catch (ServiceException e) {
                    showSingleMessage("ServiceException", "WorkerID '" + workers[i] + "': " + SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                catch (RequestErrorException e) {
                    showSingleMessage("RequestErrorException", "WorkerID '" + workers[i] + "': " + REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                catch (NullPointerException e) {
                    showSingleMessage("NullPointerException", "WorkerID '" + workers[i] + "': " + MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                catch (Exception e) {
                    showSingleMessage("AmazonMturkException", "WorkerID '" + workers[i] + "': " + MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                    return;
                }
        }
        showSingleMessage("Status", "Workers UnBlocked", JOptionPane.INFORMATION_MESSAGE);
        txtBlkReason.setText("");
        txtBlockWorkerIDs.setText("");
    }

    private void btnListBlockedWorkersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListBlockedWorkersMouseClicked
        ListWorkerBlocksRequest request = new ListWorkerBlocksRequest();

        try {
            ListWorkerBlocksResult result = client.listWorkerBlocks(request);

            String[] blockedWorkers = new String[result.getWorkerBlocks().size()];

            for (int i = 0; i < blockedWorkers.length; i++) {
                blockedWorkers[i] = result.getWorkerBlocks().get(i).getWorkerId();
            }

            lstBlockedWorkers.setListData(blockedWorkers);
        }
        catch (ServiceException e) {
            showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (RequestErrorException e) {
            showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnListBlockedWorkersMouseClicked

    private void btnUpdateHITMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnUpdateHITMouseClicked
        try {
            String hitDetailID = ghitHM.get(lstListHITs.getSelectedValue());
            GetHITRequest request = new GetHITRequest();
            request.setHITId(hitDetailID);
            GetHITResult result = client.getHIT(request);

            HIT hit = result.getHIT();

            int second = (Integer) spnSecondDetail.getValue();
            int minute = (Integer) spnMinuteDetail.getValue();
            int hour = (Integer) spnHourDetail.getValue();
            int day = (Integer) spnDayDetail.getValue();
            int month = (Integer) spnMonthDetail.getValue() - 1;
            int year = (Integer) spnYearDetail.getValue() - 1900;
            Date hitExpiration = new Date(year, month, day, hour, minute, second);

            // hit expire should only update when time changed
            if (!(hit.getExpiration().compareTo(hitExpiration) == 0)) {
                UpdateExpirationForHITRequest updateExpireRequest = new UpdateExpirationForHITRequest();
                updateExpireRequest.setHITId(hitDetailID);
                updateExpireRequest.setExpireAt(hitExpiration);
                UpdateExpirationForHITResult updateExpireResult = client.updateExpirationForHIT(updateExpireRequest);
            }

            if (!txtMaxAssignmentsDetail.getText().isEmpty()) {
                CreateAdditionalAssignmentsForHITRequest addAssignsRequest = new CreateAdditionalAssignmentsForHITRequest();
                addAssignsRequest.setHITId(hitDetailID);
                addAssignsRequest.setNumberOfAdditionalAssignments(Integer.parseInt(txtMaxAssignmentsDetail.getText()));
                CreateAdditionalAssignmentsForHITResult addAssignsResult = client.createAdditionalAssignmentsForHIT(addAssignsRequest);
            }
            int selected = lstListHITs.getSelectedIndex();
            btnListHITsMouseClicked(evt);
            lstListHITs.setSelectedIndex(selected);
            setHITDetails();
            showSingleMessage("Status", "HIT Updated", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (ServiceException e) {
            showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (RequestErrorException e) {
            showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnUpdateHITMouseClicked

    private void btnListHITsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListHITsMouseClicked
        ListHITsRequest request = new ListHITsRequest();

        try {
            populateHITs(lstListHITs);
        }
        catch (ServiceException e) {
            showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (RequestErrorException e) {
            showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnListHITsMouseClicked

    private void btnExpireHITMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnExpireHITMouseClicked
        try {
            String HITDetailID = ghitHM.get(lstListHITs.getSelectedValue());
            UpdateExpirationForHITRequest request = new UpdateExpirationForHITRequest();
            Date dateInPast = new Date(117, 1, 1, 1, 1, 1);
            request.setHITId(HITDetailID);
            request.setExpireAt(dateInPast);
            client.updateExpirationForHIT(request);
            int selected = lstListHITs.getSelectedIndex();
            btnListHITsMouseClicked(evt);
            lstListHITs.setSelectedIndex(selected);
            setHITDetails();
            showSingleMessage("Status", "HIT Expired", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (ServiceException e) {
            showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (RequestErrorException e) {
            showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception e) {
            showSingleMessage("AmazonMturkException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnExpireHITMouseClicked

    private void btnOpenWebsiteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnOpenWebsiteMouseClicked
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            String endpoint = CbxEndpoint.getModel().getSelectedItem().toString().split("[\\(\\)]")[1];
            try {
                URL url = new URL("https://" + endpoint);
                desktop.browse(url.toURI());
            } catch (Exception e) {
                showSingleMessage("Exception", "Error opening web page", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            showSingleMessage("Browse on desktop", "Not supported", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_btnOpenWebsiteMouseClicked

    private void btnLoadCredentialsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLoadCredentialsMouseClicked
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(OpenMturk.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line;
                br.readLine();
                line = br.readLine();
                String[] credentials = line.split(",");
                txtAccessKey.setText(credentials[0]);
                txtSecretKey.setText(credentials[1]);
            } catch (Exception e) {
                showSingleMessage("Load credentials", "Problem loading credentials, ensure CSV file has not been tampered since download", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnLoadCredentialsMouseClicked

    private void btnSignoutMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSignoutMouseClicked
        try {
            client.shutdown();
            txtAccessKey.setText("");
            txtSecretKey.setText("");
            txtAccountBalance.setText("");
            pnlMain.setEnabled(false);
            setSignInIndicators(Color.RED);
            showSingleMessage("Status", "Signed Out", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (NullPointerException e) {
            showSingleMessage("NullPointerException", MISC_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSignoutMouseClicked

    private void cmbxActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbxActionActionPerformed
        if (cmbxAction.getSelectedItem() == "Block Workers") {
            btnBlockWorker.setText("Block Workers");
        }
        else if (cmbxAction.getSelectedItem() == "UnBlock Workers") {
            btnBlockWorker.setText("UnBlock Workers");
        }
    }//GEN-LAST:event_cmbxActionActionPerformed

    private void btnListBonusHITsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListBonusHITsMouseClicked
        Util.clearJlist(lstWorkerIDsBonus);
        Util.clearJlist(lstAssignmentIDsBonus);
        populateHITs(lstBonusHITs);
    }//GEN-LAST:event_btnListBonusHITsMouseClicked

    private void btnSelectAllWorkersBonusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSelectAllWorkersBonusMouseClicked
        int start = 0;
        int end = lstWorkerIDsBonus.getModel().getSize() - 1;
        lstWorkerIDsBonus.setSelectionInterval(start, end);
    }//GEN-LAST:event_btnSelectAllWorkersBonusMouseClicked
    
    private void btnListHITsAssignmentMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListHITsAssignmentMouseClicked
        populateHITs(lstAssignmentHITs);
    }//GEN-LAST:event_btnListHITsAssignmentMouseClicked

    private void btnListHITsContactMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListHITsContactMouseClicked
        populateHITs(lstHITsContact);
    }//GEN-LAST:event_btnListHITsContactMouseClicked

    private void updateEstimatedCost() {
        List<String[]> errorList = new ArrayList<String[]>();
        int maxAssignments = 0;
        try {
            maxAssignments = Integer.parseInt(txtMaxAssignments.getText());
        }
        catch (NumberFormatException e) {
            String[] error = {
                "Max Assignments",
                "'" + txtMaxAssignments.getText() + "' is not a valid number!"
            };
            errorList.add(error);
        }
        String rewardString = txtReward.getText().trim();
        if (!rewardString.matches(DOLLAR_REGEX)) {
            String[] error = {
                "Reward",
                "is invalid"
            };
            errorList.add(error);
        }
        if (errorList.isEmpty()) {
            lblEstimatedCost.setText("Estimated Cost:" + " $" + Util.calculatePrice(Double.parseDouble(rewardString), maxAssignments, checkMasters(), chkMicroBatch.isSelected()));
        }
        else {
            lblEstimatedCost.setText("Estimated Cost:" + " Undefined, check inputs");
        }
    }
    
    private boolean checkMasters() {
        boolean masters = false;
        if (gQreqHM.containsKey("Masters")) {
            masters = true;
        }
        return masters;
    }
    
    private void btnCreateHITMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCreateHITMouseClicked
        List<String[]> errorList = new ArrayList<String[]>();

        CreateHITRequest request = new CreateHITRequest();

        int maxAssignments = 0;
        
        if (validHITTitle(txtHITTitle.getText().trim())) {
            request.setTitle(txtHITTitle.getText().trim());
        }
        else {
            String[] error = {
                "Hit Title",
                "You already have a HIT with that title!"
            };
            errorList.add(error);
        }

        try {
            maxAssignments = Integer.parseInt(txtMaxAssignments.getText());
            if (maxAssignments < 1 || maxAssignments > 1000000000) {
                throw new NumberFormatException();
            }
            request.setMaxAssignments(maxAssignments);
        }
        catch (NumberFormatException e) {
            String[] error = {
                "Max Assignments",
                "'" + txtMaxAssignments.getText() + "' is not a valid number!"
            };
            errorList.add(error);
        }

        try {
            float assignmentDuration = Float.parseFloat(txtAssignDuration.getText())*60;
            if (assignmentDuration < 30 || assignmentDuration > 31536000) {
                throw new NumberFormatException();
            }
            request.setAssignmentDurationInSeconds((long)assignmentDuration);
        }
        catch (NumberFormatException e) {
            String[] error = {
                "Assignment Duration",
                "'" + txtAssignDuration.getText() + "' is not a valid number!"
            };
            errorList.add(error);
        }

        if (txtAutoApprvlDelay.getText().trim().length() > 0) {
            try {
                float autoApprovalDelay = Float.parseFloat(txtAutoApprvlDelay.getText())*3600;
                if (autoApprovalDelay < 0 || autoApprovalDelay > 2592000) {
                    throw new NumberFormatException();
                }
                request.setAutoApprovalDelayInSeconds((long)autoApprovalDelay);
            }
            catch (NumberFormatException e) {
                String[] error = {
                    "Auto Approve Delay",
                    "'" + txtAutoApprvlDelay.getText() + "' is not a valid number!"
                };
                errorList.add(error);
            }
        }

        if (txtDescription.getText().trim().length() > 0) {
            request.setDescription(txtDescription.getText());
        }
        else {
            String[] error = {
                "Description",
                "'You need a description!"
            };
            errorList.add(error);
        }
        request.setKeywords(txtKeywords.getText());

        try {
            float hitLifetime = Float.parseFloat(txtHITLifetime.getText())*3600;
            if (hitLifetime < 30 || hitLifetime > 31536000) {
                throw new NumberFormatException();
            }
            request.setLifetimeInSeconds((long)hitLifetime);
        }
        catch (NumberFormatException e) {
            String[] error = {
                "Lifetime",
                "'" + txtHITLifetime.getText() + "' is not a valid number!"
            };
            errorList.add(error);
        }

        String externalURL = "";

        try {
            String urlRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
            Pattern patt = Pattern.compile(urlRegex);
            Matcher matcher = patt.matcher(txtExternalURL.getText());

            if (matcher.matches()) {
                externalURL = URLEncoder.encode(txtExternalURL.getText(), "utf-8");
                String formattedQuestion = "<HTMLQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd\">\n"
                + "<HTMLContent><![CDATA[\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head>\n" +
                "  <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>\n" +
                "  <script type='text/javascript' src='https://s3.amazonaws.com/mturk-public/externalHIT_v1.js'></script>\n" +
                " </head>\n" +
                " <body>\n" +
                "  <form name='mturk_form' method='post' id='mturk_form' action='https://www." + submitEndpoint + "mturk.com/mturk/externalSubmit'>\n" +
                "  <input type='hidden' value='' name='assignmentId' id='assignmentId'/>\n" +
                "  <h1>Please enter your completion code below after completing the survey\n" + txtExternalURL.getText() + "</h1>\n" +
                "  <p><textarea name='txtCompletionCode' cols='80' rows='3'></textarea></p>\n" +
                "  <p><input type='submit' id='submitButton' value='Submit' /></p></form>\n" +
                "  <script language='Javascript'>turkSetAssignmentID();</script>\n" +
                " </body>\n" +
                "</html>\n" +
                "]]>\n" +
                "  </HTMLContent>\n" +
                "  <FrameHeight>450</FrameHeight>\n" +
                "</HTMLQuestion>";
                request.setQuestion(formattedQuestion);
            }
            else {
                String[] error = {
                    "External URL",
                    "'" + txtExternalURL.getText() + "' is not a valid URL!"
                };
                errorList.add(error);
            }
        } catch (UnsupportedEncodingException e) {
            String[] error = {
                "External URL",
                "'" + txtExternalURL.getText() + "' is not a valid URL!"
            };
            errorList.add(error);
        }

        String rewardString = txtReward.getText().trim();

        if (rewardString.length() <= 0) {
            String[] error = {
                "Reward",
                "'You need a reward!"
            };
            errorList.add(error);
        }
        //if it's a positive number
        else if (rewardString.matches(DOLLAR_REGEX)) {
            request.setReward(rewardString);
        }
        else {
            String[] error = {
                "Reward",
                "'" + rewardString + "' is not a valid amount!"
            };
            errorList.add(error);
        }
        
        //don't move on to qualifications if there are errors
        if (errorList.size() > 0) {
            showMessage(errorList, JOptionPane.WARNING_MESSAGE);
            return;
        }

        String hitIds = "";

        int totalBatches = (maxAssignments + 9 - 1) / 9;

        ArrayList hitReqs = new ArrayList<>(gQreqHM.values());

        //***if hyperbatch***
        if (totalBatches > 1 && chkMicroBatch.isSelected()) {
            int confirm = JOptionPane.showConfirmDialog(pnlCreateHIT, "Creating this Study will cost $" + Util.calculatePrice(Double.parseDouble(rewardString), maxAssignments, checkMasters(), chkMicroBatch.isSelected()) + ". Continue?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == 0) {
                ArrayList qReqs = new ArrayList(); //store "not" QualificationRequirements in list

                // create QualificationTypes for each batch.
                for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
                    // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/mturk/model/CreateQualificationTypeRequest.html
                    String qualificationName = txtHITTitle.getText() + "-batch" + batchNum;
                    CreateQualificationTypeRequest qualification =
                    new CreateQualificationTypeRequest().withName(qualificationName)
                    .withAutoGranted(true)
                    .withAutoGrantedValue(1)
                    .withDescription("Qualification for '" + qualificationName + "'")
                    .withQualificationTypeStatus(QualificationTypeStatus.Active);

                    try {
                        CreateQualificationTypeResult qResult = client.createQualificationType(qualification);

                        QualificationType qualificationType = qResult.getQualificationType();
                        String id = qualificationType.getQualificationTypeId();

                        // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/mturk/model/Comparator.html
                        QualificationRequirement qReq = new QualificationRequirement()
                        .withQualificationTypeId(id).withComparator("DoesNotExist");
                        qReqs.add(qReq);
                    }
                    catch (RequestErrorException e) {

                        String[] error = {
                            "CreateQualificationTypeRequest",
                            "Couldn't create QualificationType '" + qualificationName + "'!"
                        };

                        errorList.add(error);
                    }
                }

                //don't move on if there are errors
                if (errorList.size() > 0) {
                    showMessage(errorList, JOptionPane.WARNING_MESSAGE);
                    return;
                }

                for (int batchNum = 0; batchNum < totalBatches; batchNum++) {

                    CreateHITRequest newRequest = request.clone();
                    newRequest.setTitle(newRequest.getTitle() + "-batch" + batchNum);

                    //if not last batch
                    if (batchNum < totalBatches - 1) {
                        newRequest.setMaxAssignments(9);
                    }
                    else { //if last batch
                        int remainingAssignments = maxAssignments % 9;
                        if (remainingAssignments == 0) // e.g. 27/9=0 (3 batches, even)
                        remainingAssignments = 9;
                        newRequest.setMaxAssignments(remainingAssignments);
                    }

                    ArrayList myQReqs = (ArrayList) qReqs.clone();
                    myQReqs.remove(batchNum);
                    QualificationRequirement hasReq = new QualificationRequirement().withQualificationTypeId(((QualificationRequirement)qReqs.get(batchNum)).getQualificationTypeId()).withComparator("Exists");
                    myQReqs.add(hasReq);

                    myQReqs.addAll(hitReqs);
                    newRequest.setQualificationRequirements(myQReqs);

                    try {
                        CreateHITResult result = client.createHIT(newRequest);
                        HIT hit = result.getHIT();
                        hitIds += hit.getHITId() + ", ";
                    }
                    catch (ServiceException e) {
                        showSingleMessage("ServiceException", "batch-" + batchNum + ": " +  SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    catch (RequestErrorException e) {
                        showSingleMessage("RequestErrorException", "batch-" + batchNum + ": " +  REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                showSingleMessage("Status", "HITs " + " Created", JOptionPane.INFORMATION_MESSAGE);
            }
        } //***not hyperbatch***
        else {
            try {
                int confirm = JOptionPane.showConfirmDialog(pnlCreateHIT, "Creating this HIT will cost $" + Util.calculatePrice(Double.parseDouble(rewardString), maxAssignments, checkMasters(), chkMicroBatch.isSelected()) + ". Continue?", "Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    request.setQualificationRequirements(hitReqs);
                    CreateHITResult result = client.createHIT(request);
                    showSingleMessage("Status", "HIT " + result.getHIT().getHITId() + " Created", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            catch (ServiceException e) {
                showSingleMessage("ServiceException", SERVICE_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
            }
            catch (RequestErrorException e) {
                showSingleMessage("RequestErrorException", REQUEST_EXCEPTION_MSG, JOptionPane.ERROR_MESSAGE);
            }
        }
        gErrorMessageList = null;
    }//GEN-LAST:event_btnCreateHITMouseClicked

    private void btnUpdateAssignmentsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnUpdateAssignmentsMouseClicked
        populateHITs(lstListHITs);
        setAssignments();
    }//GEN-LAST:event_btnUpdateAssignmentsMouseClicked

    private void txtMaxAssignmentsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtMaxAssignmentsFocusLost
        updateEstimatedCost();
    }//GEN-LAST:event_txtMaxAssignmentsFocusLost

    private void txtRewardFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtRewardFocusLost
        updateEstimatedCost();
    }//GEN-LAST:event_txtRewardFocusLost

    private void chkMicroBatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkMicroBatchActionPerformed
        updateEstimatedCost();
    }//GEN-LAST:event_chkMicroBatchActionPerformed

    private void btnDownloadCSVBonusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDownloadCSVBonusMouseClicked
        if (Util.getListData(lstWorkerIDsBonus).length == 0) {
            showSingleMessage("Download CSV", "No worker/assignment ids loaded, please select a Survey", JOptionPane.ERROR_MESSAGE);
        }
        else {
           try {
            String[] workers = Util.getListData(lstWorkerIDsBonus);
            String[] assignments = Util.getListData(lstAssignmentIDsBonus);
            HashMap<String,String[]> data = new HashMap<>();
            data.put("Workers", workers);
            data.put("Assignments", assignments);
            JFileChooser saveFileDlg = new JFileChooser();
            saveFileDlg.setDialogTitle("Specify a path for your csv");
            int userSelection = saveFileDlg.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = saveFileDlg.getSelectedFile();
                Util.generateCSV(fileToSave, data);
                showSingleMessage("Information", "CSV saved to " + fileToSave.getAbsolutePath(), JOptionPane.INFORMATION_MESSAGE);
            }
            } catch (Exception e) {
                showSingleMessage("Download CSV", "Error writing to file, check your file permissions", JOptionPane.ERROR_MESSAGE);
            } 
        }
        
        
    }//GEN-LAST:event_btnDownloadCSVBonusMouseClicked

    private void btnUploadCSVBonusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnUploadCSVBonusMouseClicked
        try {
            Util.clearJlist(lstBonusHITs);
            Util.clearJlist(lstWorkerIDsBonus);
            Util.clearJlist(lstAssignmentIDsBonus);
            JFileChooser loadCSVDlg = new JFileChooser();
            loadCSVDlg.setDialogTitle("Select a CSV to upload");
            loadCSVDlg.setMultiSelectionEnabled(false);
            int userSelection = loadCSVDlg.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                String filePath = loadCSVDlg.getSelectedFile().getAbsolutePath();
                List<String[]> data = Util.loadCSV(filePath);
                String[] workers = new String[data.size()];
                String[] assignments = new String[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    workers[i] = data.get(i)[0];
                    assignments[i] = data.get(i)[1];
                }
                lstWorkerIDsBonus.setListData(workers);
                lstAssignmentIDsBonus.setListData(assignments);
            }
        } catch (Exception e) {
            showSingleMessage("Upload CSV", "Error uploading CSV, ensure your CSV file has a heading and uses proper formatting", JOptionPane.ERROR_MESSAGE);
            System.out.println(e);
        }
        
    }//GEN-LAST:event_btnUploadCSVBonusMouseClicked

    private void btnSaveCredentialsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSaveCredentialsMouseClicked
        if (!(txtAccessKey.getText().isEmpty() || txtSecretKey.getText().isEmpty())) {
            int selection = JOptionPane.showConfirmDialog(this, "Saving credentials on a public computer can pose a security risk, are you sure you wish to save your credentials?", "Warning", JOptionPane.YES_NO_OPTION);
            if (selection == JOptionPane.YES_OPTION) {
                saveCredentials();
                showSingleMessage("File Status", "Credentials saved successfully", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        else {
            showSingleMessage("File Status", "Credentials must be filled in in order to save them!", JOptionPane.ERROR_MESSAGE);
        }
        
    }//GEN-LAST:event_btnSaveCredentialsMouseClicked

    private void btnDeleteCredentialsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteCredentialsMouseClicked
        try {
            Files.deleteIfExists(Paths.get("credentials.txt"));
            showSingleMessage("File Status", "Credentials successfully deleted", JOptionPane.INFORMATION_MESSAGE);
            txtAccessKey.setText("");
            txtSecretKey.setText("");
        }
        catch (Exception e) {
            showSingleMessage("File Status", "Unable to delete credentials", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDeleteCredentialsMouseClicked

    private void btnCreateQualificationMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCreateQualificationMouseClicked
        try {
            CreateQualificationTypeRequest request = new CreateQualificationTypeRequest();
            request.setName(txtQualificationName.getText());
            request.setDescription(txtQualficationDescription.getText());
            String keywords = txtQualificationKeywords.getText().replaceAll("\\s+","");
            request.setKeywords(keywords);
            request.setAutoGranted(chkAutoGranted.isSelected());
            request.setQualificationTypeStatus(QualificationTypeStatus.Active);
            client.createQualificationType(request);
            btnListQualificationMouseClicked(evt);
            showSingleMessage("Qualifications", "Qualification created successfully!", JOptionPane.INFORMATION_MESSAGE);
            txtQualificationName.setText("");
            txtQualficationDescription.setText("");
            txtQualificationKeywords.setText("");
        }
        catch (Exception e) {
            showSingleMessage("Qualification Status", "Unable to create qualification type, check your inputs", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnCreateQualificationMouseClicked

    private void btnListQualificationMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnListQualificationMouseClicked
        try {
            ListQualificationTypesRequest request = new ListQualificationTypesRequest();
            request.setMustBeOwnedByCaller(true);
            request.setMustBeRequestable(true);
            ListQualificationTypesResult result = client.listQualificationTypes(request);
            List<QualificationType> qtypes = result.getQualificationTypes();
            HashMap<String, String> qtypeHM = new HashMap<>();
            for (int i = 0; i < qtypes.size(); i++) {
                String qtypeID = qtypes.get(i).getQualificationTypeId();
                String qtypeName = qtypes.get(i).getName();
                qtypeHM.put(qtypeName, qtypeID);
            }
            String qtypeNames[] = (String[])qtypeHM.keySet().toArray(new String[qtypeHM.size()]);
            lstQualificationTypes.setListData(qtypeNames);
            gQualificationHM = qtypeHM;
        } catch (Exception e) {
            showSingleMessage("Qualifications", "Unable to fetch qualification types", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnListQualificationMouseClicked

    private void btnDeleteQualificationMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteQualificationMouseClicked
        try {
            String qtypeID = gQualificationHM.get(lstQualificationTypes.getSelectedValue());
            DeleteQualificationTypeRequest request = new DeleteQualificationTypeRequest();
            request.setQualificationTypeId(qtypeID);
            client.deleteQualificationType(request);
            btnListQualificationMouseClicked(evt);
            showSingleMessage("Qualifications", "Qualification deleted successfully!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showSingleMessage("Qualifications", "Unable to delete qualification type", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDeleteQualificationMouseClicked

    private void btnUpdateQualificationMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnUpdateQualificationMouseClicked
        try {
            if (!lstQualificationTypes.isSelectionEmpty()) {
                String qtypeID = gQualificationHM.get(lstQualificationTypes.getSelectedValue());
                UpdateQualificationTypeRequest request = new UpdateQualificationTypeRequest();
                request.setQualificationTypeId(qtypeID);
                request.setDescription(txtQualficationDescription.getText());
                request.setAutoGranted(chkAutoGranted.isSelected());
                client.updateQualificationType(request);
                showSingleMessage("Qualifications", "Qualification updated successfully!", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                showSingleMessage("Qualifications", "You must select a qualification to update", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            showSingleMessage("Qualification Status", "Unable to update qualification type, check your inputs", JOptionPane.ERROR_MESSAGE);
        }
        
    }//GEN-LAST:event_btnUpdateQualificationMouseClicked

    private void btnModifyQualificationRequirementsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnModifyQualificationRequirementsMouseClicked
        HashMap<String, String> systemQtypeHM = getSystemQualificationMappings();
        QualificationForm fm = new QualificationForm(this, true, client, systemQtypeHM, gQreqHM);
        fm.setVisible(rootPaneCheckingEnabled);
        gQreqHM = fm.getQualificationRequirements();
        Set<String> keys = gQreqHM.keySet();
        String qReqNames[] = keys.toArray(new String[keys.size()]);
        lstHITQualificationRequirements.setListData(qReqNames);
        fm.dispose();
        if (!gQreqHM.isEmpty()) {
            chkMicroBatch.setSelected(false);
            chkMicroBatch.setEnabled(false);
        }
        else {
            chkMicroBatch.setEnabled(true);
        }
    }//GEN-LAST:event_btnModifyQualificationRequirementsMouseClicked

    private void lstHITQualificationRequirementsPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_lstHITQualificationRequirementsPropertyChange
        updateEstimatedCost();
    }//GEN-LAST:event_lstHITQualificationRequirementsPropertyChange
    
    private boolean validHITTitle(String title) {
        List<String> hits = new ArrayList<String>(listHITs().keySet());
        for (int i = 0; i < hits.size(); i++) {
            if (hits.get(i).equals(title)) {
                return false;
            }
        }
        return true;
    }
    
    private void populateHITs(JList list) {
        HashMap<String, String> hitHashMap = listHITs();
        
        String hitTitles[] = (String[])hitHashMap.keySet().toArray(new String[hitHashMap.size()]);

        list.setListData(hitTitles);
        ghitHM = hitHashMap;
    }
    
    private HashMap<String, String> listHITs() {
        HashMap<String, String> hitHashMap = new HashMap<>();
        ListHITsRequest request = new ListHITsRequest();
        ListHITsResult result = client.listHITs(request);
        List<HIT> hits = result.getHITs();
        for (int i = 0; i < hits.size(); i++) {
            String hitID = hits.get(i).getHITId();
            String hitName = hits.get(i).getTitle();
            hitHashMap.put(hitName, hitID);
        }
        return hitHashMap;
    }
    
    private void setQualificationTypeDetails() {
        if (!lstQualificationTypes.isSelectionEmpty()) {
            String qtypeID = gQualificationHM.get(lstQualificationTypes.getSelectedValue());
            GetQualificationTypeRequest request = new GetQualificationTypeRequest();
            request.setQualificationTypeId(qtypeID);
            GetQualificationTypeResult result = client.getQualificationType(request);
            QualificationType qtype = result.getQualificationType();
            
            txtQualificationName.setText(qtype.getName());
            txtQualficationDescription.setText(qtype.getDescription());
            txtQualificationKeywords.setText(qtype.getKeywords());
            chkAutoGranted.setSelected(qtype.getAutoGranted());
        }
    }
    
    private void setHITDetails() {
        if (!lstListHITs.isSelectionEmpty()) {
            String hitID = ghitHM.get(lstListHITs.getSelectedValue());
            GetHITRequest request = new GetHITRequest();
            request.setHITId(hitID);
            GetHITResult result = client.getHIT(request);
            HIT hit = result.getHIT();

            lblHITTitleDetail.setText("Hit Title: " + hit.getTitle());
            lblAssignmentDurationDetail.setText("Assignment Duration: " + hit.getAssignmentDurationInSeconds()/60 + " minute(s)");
            lblAutoApprovalDelayDetail.setText("Auto Approval Delay: " + hit.getAutoApprovalDelayInSeconds()/3600 + " hour(s)");
            lblCreationTimeDetail.setText("HIT Creation Time: " + hit.getCreationTime());

            Date hitExpiration = hit.getExpiration();
            spnSecondDetail.setValue(hitExpiration.getSeconds());
            spnMinuteDetail.setValue(hitExpiration.getMinutes());
            spnHourDetail.setValue(hitExpiration.getHours());
            spnDayDetail.setValue(hitExpiration.getDate());
            spnMonthDetail.setValue(hitExpiration.getMonth() + 1);
            spnYearDetail.setValue(hitExpiration.getYear() + 1900);

            lblHITGroupIDDetail.setText("HIT Group ID: " + hit.getHITGroupId());
            lblHITLayoutIDDetail.setText("HIT Layout ID: " + hit.getHITLayoutId());
            lblHITReviewStatusDetail.setText("HIT Review Status: " + hit.getHITReviewStatus());
            lblHITStatusDetail.setText("HIT Status: " + hit.getHITStatus());
            lblHITTypeIDDetail.setText("HIT Type ID: " + hit.getHITTypeId());
            lblKeywordsDetail.setText("Keywords: " + hit.getKeywords());
            lblMaxAssignmentsDetail.setText("Max Assignments: " + hit.getMaxAssignments().toString());
            lblRewardDetail.setText("Reward: " + hit.getReward());
            lblHITDescriptionDetail.setText("Description: " + hit.getDescription());
        }
    }

    protected void setAssignmentDetails() {
        if (!lstAssignments.isSelectionEmpty()) {
            Assignment assignment = assignments.get(lstAssignments.getSelectedIndex());
            lblAssignmentID.setText("Assignment: " + assignment.getAssignmentId());
            lblWorkerID.setText("Worker ID: " + assignment.getWorkerId());
            lblHITIDDetail.setText("HIT ID: " + assignment.getWorkerId());
            lblAssignmentStatus.setText("Assignment Status: " + assignment.getAssignmentStatus());
            lblAutoApprovalTime.setText("Auto Approval Time: " + assignment.getAutoApprovalTime());
            lblAcceptTime.setText("Accept Time: " + assignment.getAcceptTime());
            lblSubmitTime.setText("Submit Time: " + assignment.getSubmitTime());
            lblApprovalTime.setText("Approval Time: " + assignment.getApprovalTime());
            lblRejectionTime.setText("Rejection Time: " + assignment.getRejectionTime());
            lblDeadline.setText("Deadline: " + assignment.getDeadline());
        }
    }
    
    private void setAssignments() {
        if (!lstAssignmentHITs.isSelectionEmpty()) {
            String hitID = ghitHM.get(lstAssignmentHITs.getSelectedValue());
            ListAssignmentsForHITRequest request = new ListAssignmentsForHITRequest().withAssignmentStatuses(Collections.singletonList(AssignmentStatus.Submitted.name()));
            request.setHITId(hitID);
            
            ListAssignmentsForHITResult result = client.listAssignmentsForHIT(request);

            assignments = result.getAssignments();
            String[] assignmentIDs = new String[assignments.size()];
            String[] noAssignments = new String[]{"No Assignments"};
            if (assignmentIDs.length == 0) {
                lstAssignments.setListData(noAssignments);
                lstAssignments.setEnabled(false);
            }
            else {
                for (int i = 0; i < assignments.size(); i++) {
                    assignmentIDs[i] = assignments.get(i).getAssignmentId();
                }
                lstAssignments.setListData(assignmentIDs);
                lstAssignments.setEnabled(true);
            }
        }
    }
    
    private void setWorkersContact() {
        if (!lstHITsContact.isSelectionEmpty()) {
            txtRecipients.setText("");
            String hitID = ghitHM.get(lstHITsContact.getSelectedValue());
            ListAssignmentsForHITRequest request = new ListAssignmentsForHITRequest();
            request.setHITId(hitID);
            request.setAssignmentStatuses(Collections.singletonList(AssignmentStatus.Approved.name()));
            ListAssignmentsForHITResult result = client.listAssignmentsForHIT(request);
            List<Assignment> assignmentsList = result.getAssignments();
            String[] assignmentIDs = new String[assignmentsList.size()];
            String[] workerIDs = new String[assignmentsList.size()];
            for (int i = 0; i < assignmentsList.size(); i++) {
                assignmentIDs[i] = assignmentsList.get(i).getAssignmentId();
                workerIDs[i] = assignmentsList.get(i).getWorkerId();
            }
            for (int i = 0; i < workerIDs.length; i++) {
                txtRecipients.setText(txtRecipients.getText() + workerIDs[i] + "\n");
            }
        }
    }
    
    private void setBonusDetails() {
        if (!lstBonusHITs.isSelectionEmpty()) {
            String hitID = ghitHM.get(lstBonusHITs.getSelectedValue());
            ListAssignmentsForHITRequest request = new ListAssignmentsForHITRequest();
            request.setHITId(hitID);
            request.setAssignmentStatuses(Collections.singletonList(AssignmentStatus.Approved.name()));
            ListAssignmentsForHITResult result = client.listAssignmentsForHIT(request);
            List<Assignment> assignmentsList = result.getAssignments();
            String[] assignmentIDs = new String[assignmentsList.size()];
            String[] workerIDs = new String[assignmentsList.size()];
            for (int i = 0; i < assignmentsList.size(); i++) {
                assignmentIDs[i] = assignmentsList.get(i).getAssignmentId();
                workerIDs[i] = assignmentsList.get(i).getWorkerId();
            }
            lstAssignmentIDsBonus.setListData(assignmentIDs);
            lstWorkerIDsBonus.setListData(workerIDs);
        }
    }

    private void showMessage(List<String[]> messages, int messageType) {
        MessageList messageList;

        if (gErrorMessageList == null) {
            messageList = new MessageList(messages);
        }
        else {
            messageList = gErrorMessageList;
        }

        messageList.setLayout(new GridLayout(0,1));

        //default for ERROR_MESSAGE or WARNING_MESSAGE
        String title = "Errors encountered";

        if (messageType == JOptionPane.PLAIN_MESSAGE || messageType == JOptionPane.INFORMATION_MESSAGE) {
            title = "Information";
        }

        JOptionPane.showMessageDialog(this, messageList, title, messageType);
    }

    private void showSingleMessage(String title, String message, int messageType) {
        List<String[]> errorList = new ArrayList<String[]>();
        String[] error = { title, message };
        errorList.add(error);
        showMessage(errorList, messageType);
    }
    
    private void setSignInIndicators(Color color) {
        String status = "";
        if (color == Color.RED) {
            status = "Logged Out";
        }
        else {
            status = "Logged In";
        }
        javax.swing.JPanel[] signInPanels = {
            pnlSignInAccount,
            pnlSignInAssignment,
            pnlSignInBlockUnBlockWorkers,
            pnlSignInBonuses,
            pnlSignInContactWorkers,
            pnlSignInCreateHIT,
            pnlSignInHITDetail,
            pnlSignInQualification
        };
        javax.swing.JLabel[] signInLabels = {
            lblSignInAccount,
            lblSignInAssignment,
            lblSignInBlockUnBlockWorkers,
            lblSignInWorkerBonus,
            lblSignInContactWorkers,
            lblSignInCreateHIT,
            lblSignInHITDetail,
            lblSignInQualification
        };
        for (int i = 0; i < signInPanels.length; i++) {
            signInPanels[i].setBackground(color);
            signInLabels[i].setText(status);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OpenMturk.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OpenMturk.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OpenMturk.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OpenMturk.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OpenMturk().setVisible(true);
            }
        });
    }

    public class SharedListSelectionHandler implements ListSelectionListener {
        JList actionList;

        public SharedListSelectionHandler(JList list) {
            actionList = list;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting() && actionList.getName() == "lstAssignments") {
                setAssignmentDetails();
            }
            else if (!e.getValueIsAdjusting() && actionList.getName() == "lstAssignmentHITs") {
                setAssignments();
            }
            else if (!e.getValueIsAdjusting() && actionList.getName() == "lstListHITs") {
                setHITDetails();
            }
            else if (!e.getValueIsAdjusting() && actionList.getName() == "lstBonusHITs") {
                setBonusDetails();
            }
            else if (!e.getValueIsAdjusting() && actionList.getName() == "lstHITsContact") {
                setWorkersContact();
            }
            else if (!e.getValueIsAdjusting() && actionList.getName() == "lstQualificationTypes") {
                setQualificationTypeDetails();
            }
        }
    }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> CbxEndpoint;
    private javax.swing.JButton btnApproveAll;
    private javax.swing.JButton btnApproveSelected;
    private javax.swing.JButton btnBlockWorker;
    private javax.swing.JButton btnContactMessage;
    private javax.swing.JButton btnCreateHIT;
    private javax.swing.JButton btnCreateQualification;
    private javax.swing.JButton btnDeleteCredentials;
    private javax.swing.JButton btnDeleteHIT;
    private javax.swing.JButton btnDeleteQualification;
    private javax.swing.JButton btnDownloadCSVBonus;
    private javax.swing.JButton btnExpireHIT;
    private javax.swing.JButton btnGetBalance;
    private javax.swing.JButton btnListBlockedWorkers;
    private javax.swing.JButton btnListBonusHITs;
    private javax.swing.JButton btnListHITs;
    private javax.swing.JButton btnListHITsAssignment;
    private javax.swing.JButton btnListHITsContact;
    private javax.swing.JButton btnListQualification;
    private javax.swing.JButton btnLoadCredentials;
    private javax.swing.JButton btnModifyQualificationRequirements;
    private javax.swing.JButton btnOpenWebsite;
    private javax.swing.JButton btnRejectSelected;
    private javax.swing.JButton btnSaveCredentials;
    private javax.swing.JButton btnSelectAllWorkersBonus;
    private javax.swing.JButton btnSendBonus;
    private javax.swing.JButton btnSignout;
    private javax.swing.JButton btnUpdateAssignments;
    private javax.swing.JButton btnUpdateHIT;
    private javax.swing.JButton btnUpdateQualification;
    private javax.swing.JButton btnUploadCSVBonus;
    private javax.swing.JButton btnValidate;
    private javax.swing.JCheckBox chkAutoGranted;
    private javax.swing.JCheckBox chkMicroBatch;
    private javax.swing.JComboBox<String> cmbxAction;
    private javax.swing.JLabel lblAWSCreds;
    private javax.swing.JLabel lblAcceptTime;
    private javax.swing.JLabel lblAccessKey;
    private javax.swing.JLabel lblAddAssignments;
    private javax.swing.JLabel lblApprovalTime;
    private javax.swing.JLabel lblAssignDuration;
    private javax.swing.JLabel lblAssignmentDetails;
    private javax.swing.JLabel lblAssignmentDurationDetail;
    private javax.swing.JLabel lblAssignmentDurationUnit;
    private javax.swing.JLabel lblAssignmentHITs;
    private javax.swing.JLabel lblAssignmentID;
    private javax.swing.JLabel lblAssignmentIDs;
    private javax.swing.JLabel lblAssignmentStatus;
    private javax.swing.JLabel lblAssignments;
    private javax.swing.JLabel lblAutoApprovalDelayDetail;
    private javax.swing.JLabel lblAutoApprovalTime;
    private javax.swing.JLabel lblAutoApprovalUnit;
    private javax.swing.JLabel lblAutoApprveDelay;
    private javax.swing.JLabel lblBlkReason;
    private javax.swing.JLabel lblBlkWorkerID;
    private javax.swing.JLabel lblBlockedWorkers;
    private javax.swing.JLabel lblBonusAmount;
    private javax.swing.JLabel lblBonusHITs;
    private javax.swing.JLabel lblCreateQualification;
    private javax.swing.JLabel lblCreationTimeDetail;
    private javax.swing.JLabel lblDeadline;
    private javax.swing.JLabel lblDescription;
    private javax.swing.JLabel lblEndpoint;
    private javax.swing.JLabel lblEstimatedCost;
    private javax.swing.JLabel lblExternalURL;
    private javax.swing.JLabel lblHITDescriptionDetail;
    private javax.swing.JLabel lblHITExpirationTimeDetail;
    private javax.swing.JLabel lblHITGroupIDDetail;
    private javax.swing.JLabel lblHITIDDetail;
    private javax.swing.JLabel lblHITLayoutIDDetail;
    private javax.swing.JLabel lblHITLifetime;
    private javax.swing.JLabel lblHITLifetimeUnit;
    private javax.swing.JLabel lblHITReviewStatusDetail;
    private javax.swing.JLabel lblHITStatusDetail;
    private javax.swing.JLabel lblHITTitle;
    private javax.swing.JLabel lblHITTitleDetail;
    private javax.swing.JLabel lblHITTypeIDDetail;
    private javax.swing.JLabel lblHITsContact;
    private javax.swing.JLabel lblHITsUpdate;
    private javax.swing.JLabel lblKeywords;
    private javax.swing.JLabel lblKeywordsDetail;
    private javax.swing.JLabel lblMaxAssignments;
    private javax.swing.JLabel lblMaxAssignmentsDetail;
    private javax.swing.JLabel lblMessage;
    private javax.swing.JLabel lblMicroBatch;
    private javax.swing.JLabel lblName;
    private javax.swing.JLabel lblNumofParticipantsUnit;
    private javax.swing.JLabel lblQualificationDesc;
    private javax.swing.JLabel lblQualificationKeywords;
    private javax.swing.JLabel lblQualifications;
    private javax.swing.JLabel lblQualificationsDetail;
    private javax.swing.JLabel lblReason;
    private javax.swing.JLabel lblRecipients;
    private javax.swing.JLabel lblRejectionTime;
    private javax.swing.JLabel lblRequesterFeedback;
    private javax.swing.JLabel lblReward;
    private javax.swing.JLabel lblRewardDetail;
    private javax.swing.JLabel lblRewardUnits;
    private javax.swing.JLabel lblSecretKey;
    private javax.swing.JLabel lblSignInAccount;
    private javax.swing.JLabel lblSignInAssignment;
    private javax.swing.JLabel lblSignInBlockUnBlockWorkers;
    private javax.swing.JLabel lblSignInContactWorkers;
    private javax.swing.JLabel lblSignInCreateHIT;
    private javax.swing.JLabel lblSignInHITDetail;
    private javax.swing.JLabel lblSignInQualification;
    private javax.swing.JLabel lblSignInWorkerBonus;
    private javax.swing.JLabel lblSubject;
    private javax.swing.JLabel lblSubmitTime;
    private javax.swing.JLabel lblWorkerID;
    private javax.swing.JLabel lblWorkerIDs;
    private javax.swing.JList<String> lstAssignmentHITs;
    private javax.swing.JList<String> lstAssignmentIDsBonus;
    private javax.swing.JList<String> lstAssignments;
    private javax.swing.JList<String> lstBlockedWorkers;
    private javax.swing.JList<String> lstBonusHITs;
    private javax.swing.JList<String> lstHITQualificationRequirements;
    private javax.swing.JList<String> lstHITsContact;
    private javax.swing.JList<String> lstListHITs;
    private javax.swing.JList<String> lstQualificationTypes;
    private javax.swing.JList<String> lstWorkerIDsBonus;
    private javax.swing.JLayeredPane lyrpnMicrobatchSelect;
    private javax.swing.JPanel pnlAccount;
    private javax.swing.JPanel pnlAppRejAssignments;
    private javax.swing.JPanel pnlBlkUblkWorkers;
    private javax.swing.JPanel pnlBonuses;
    private javax.swing.JPanel pnlContactWorkers;
    private javax.swing.JPanel pnlCreateHIT;
    private javax.swing.JPanel pnlHITDetail;
    private javax.swing.JTabbedPane pnlMain;
    private javax.swing.JPanel pnlQualifications;
    private javax.swing.JPanel pnlSignInAccount;
    private javax.swing.JPanel pnlSignInAssignment;
    private javax.swing.JPanel pnlSignInBlockUnBlockWorkers;
    private javax.swing.JPanel pnlSignInBonuses;
    private javax.swing.JPanel pnlSignInContactWorkers;
    private javax.swing.JPanel pnlSignInCreateHIT;
    private javax.swing.JPanel pnlSignInHITDetail;
    private javax.swing.JPanel pnlSignInQualification;
    private javax.swing.JScrollPane sclAssignmentHITs;
    private javax.swing.JScrollPane sclAssignmentIDsBonus;
    private javax.swing.JScrollPane sclAssignments;
    private javax.swing.JScrollPane sclBlkReason;
    private javax.swing.JScrollPane sclBlockList;
    private javax.swing.JScrollPane sclBlockWorkers;
    private javax.swing.JScrollPane sclBonusHITs;
    private javax.swing.JScrollPane sclHITQualificationRequirements;
    private javax.swing.JScrollPane sclHITsContact;
    private javax.swing.JScrollPane sclListHITs;
    private javax.swing.JScrollPane sclMessage;
    private javax.swing.JScrollPane sclQualificationDescription;
    private javax.swing.JScrollPane sclQualificationTypes;
    private javax.swing.JScrollPane sclReason;
    private javax.swing.JScrollPane sclRecipients;
    private javax.swing.JScrollPane sclRequesterFeedback;
    private javax.swing.JScrollPane sclWorkerIDsBonus;
    private javax.swing.JScrollPane sclpnDescription;
    private javax.swing.JScrollPane sclpnKeywords;
    private javax.swing.JSpinner spnDayDetail;
    private javax.swing.JSpinner spnHourDetail;
    private javax.swing.JSpinner spnMinuteDetail;
    private javax.swing.JSpinner spnMonthDetail;
    private javax.swing.JSpinner spnSecondDetail;
    private javax.swing.JSpinner spnYearDetail;
    private javax.swing.JTextField txtAccessKey;
    private javax.swing.JTextField txtAccountBalance;
    private javax.swing.JTextField txtAssignDuration;
    private javax.swing.JTextField txtAutoApprvlDelay;
    private javax.swing.JTextArea txtBlkReason;
    private javax.swing.JTextArea txtBlockWorkerIDs;
    private javax.swing.JTextField txtBonusAmount;
    private javax.swing.JTextArea txtContactMessage;
    private javax.swing.JTextField txtContactSubject;
    private javax.swing.JTextArea txtDescription;
    private javax.swing.JTextField txtExternalURL;
    private javax.swing.JTextField txtHITLifetime;
    private javax.swing.JTextField txtHITTitle;
    private javax.swing.JTextArea txtKeywords;
    private javax.swing.JTextField txtMaxAssignments;
    private javax.swing.JTextField txtMaxAssignmentsDetail;
    private javax.swing.JTextArea txtQualficationDescription;
    private javax.swing.JTextField txtQualificationKeywords;
    private javax.swing.JTextField txtQualificationName;
    private javax.swing.JTextArea txtReason;
    private javax.swing.JTextArea txtRecipients;
    private javax.swing.JTextArea txtRequesterFeedback;
    private javax.swing.JTextField txtReward;
    private javax.swing.JTextField txtSecretKey;
    // End of variables declaration//GEN-END:variables
}
