package com.SakshamBeatBox;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class BeatBox {
    JFrame theFrame;
    JPanel mainPanel;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkBoxList;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    Sequencer sequencer;
    Sequence sequence;
    Track track;

    String[] instrumentName = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Clap", "Hi Bongo", "Marcas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-Mid Tom", "High Agogo", "Open Hi Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {
        new BeatBox().startUp();
    }

    public void startUp(){
        try {
            Socket sock = new Socket("182.64.212.90", 8080);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        }catch (Exception ex){
            System.out.println("couldn't connect");
        }

        setUpMIDI();
        buildGUI();
        NameDialog();
    }

    public void NameDialog(){
        Object[] option = {"Enter"};

        JPanel panel = new JPanel();
        panel.add(new JLabel("Please Enter your Name:"));
        JTextField textField = new JTextField(20);
        panel.add(textField);

        JOptionPane.showOptionDialog(theFrame,
                panel,
                "User Name",
                JOptionPane.YES_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                option,
                option[0]);
        userName = textField.getText();
    }
    public void buildGUI(){
        theFrame = new JFrame("Cyber BeatBox");
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkBoxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton random = new JButton("Generate Random");
        random.addActionListener(new MyRandomListener());
        buttonBox.add(random);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        JButton send = new JButton("Send");
        send.addActionListener(new MySendListener());
        buttonBox.add(send);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem loadMenuItem = new JMenuItem("Load");
        JMenuItem clearMenuItem = new JMenuItem("Clear");

        saveMenuItem.addActionListener(new MySaveListener());
        loadMenuItem.addActionListener(new MyLoadListener());
        clearMenuItem.addActionListener(new MyClearListener());

        menu.add(saveMenuItem);
        menu.add(loadMenuItem);
        menuBar.add(menu);
        menuBar.add(clearMenuItem);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i=0;i<16;i++){
            nameBox.add(new Label(instrumentName[i]));
        }

        background.add(BorderLayout.WEST,nameBox);
        background.add(BorderLayout.EAST,buttonBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER,mainPanel);

        for(int i=0;i<256;i++){
            JCheckBox c =new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        theFrame.setJMenuBar(menuBar);
        theFrame.setBounds(550,300,300,300);
        theFrame.pack();
        theFrame.getRootPane().setDefaultButton(send);
        theFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        theFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ExitDialog();
            }
        });
        theFrame.setVisible(true);
    }

    public void setUpMIDI() {
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch (Exception e){e.printStackTrace();}
    }

    public void buildTrackAndStart(){
        int[] trackList;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i=0;i<16;i++){
            trackList = new int[16];

            int key = instruments[i];

            for (int j=0;j<16;j++){
                JCheckBox jc = checkBoxList.get(j+(16*i));
                if(jc.isSelected()){
                    trackList[j] = key;
                }
                else {
                    trackList[j] = 0;
                }
            }

            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));
        }

        track.add(makeEvent(192,9,1,0,15));

        try{
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        }catch (Exception e){e.printStackTrace();}
    }

    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a){
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 0.97));
        }
    }

    public class MyClearListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(int i=0;i<256;i++){
                JCheckBox jCheckBox = checkBoxList.get(i);
                jCheckBox.setSelected(false);
            }
            sequencer.stop();
        }
    }

    public class MySaveListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);
            saveFile(fileSave.getSelectedFile());
        }
    }

    public class MyLoadListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileOpen = new JFileChooser();
            fileOpen.showOpenDialog(theFrame);
            loadFile(fileOpen.getSelectedFile());
        }
    }

    public class MySendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean[] checkBoxState = new boolean[256];

            for (int i=0;i<256;i++) {
                JCheckBox checkBox = checkBoxList.get(i);

                checkBoxState[i] = checkBox.isSelected();
            }
            try {
                out.writeObject(userName + "(" + nextNum++ + ")" + ":" + userMessage.getText());
                out.writeObject(checkBoxState);
            }catch (Exception ex){
                System.out.println("Failed to send message");
            }
            userMessage.setText("");
        }
    }

    public class MyRandomListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < 256; i++) {
                JCheckBox jc = checkBoxList.get(i);

                int r = (int) (10 * Math.random());
                jc.setSelected(r < 2);
            }
            sequencer.stop();
            buildTrackAndStart();
        }
    }

    public class MyListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent le) {
            if(!le.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if(selected != null){
                    boolean[] selectedState = otherSeqsMap.get(selected);

                    int result = JOptionPane.showConfirmDialog(theFrame,
                            "Do you want to change current pattern?",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE,
                            JOptionPane.YES_NO_OPTION);
                    if(result == JOptionPane.YES_OPTION) {
                        changeSequence(selectedState);
                        sequencer.stop();
                        buildTrackAndStart();
                    }
                }
            }
        }
    }

    public class RemoteReader implements Runnable {
        boolean[] checkBoxState = null;
        Object obj = null;

        @Override
        public void run() {
            try {
                while((obj=in.readObject()) != null){
                    System.out.println("Object received");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    checkBoxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkBoxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    public void ExitDialog(){
        int result = JOptionPane.showOptionDialog(theFrame,
                "Do you want to save changes?",
                "Warning",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[] {"Save","Don't Save","Cancel"},
                "Save");

        if(result == JOptionPane.YES_OPTION){
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);
            saveFile(fileSave.getSelectedFile());
            System.exit(0);
        }else if(result == JOptionPane.NO_OPTION){
            System.exit(0);
        }
    }

    public void changeSequence (boolean[] checkBoxState){
        for(int i=0;i<256;i++){
            JCheckBox checkBox = checkBoxList.get(i);
            checkBox.setSelected(checkBoxState[i]);
        }
    }

    public void makeTracks(int[] list){
        for (int i=0;i<16;i++){
            int key = list[i];

            if(key!=0){
                track.add(makeEvent(144,9,key,100,i));
                track.add(makeEvent(128,9,key,100,i+2));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
        MidiEvent event = null;
        try {
            ShortMessage s = new ShortMessage();
            s.setMessage(comd, chan, one, two);
            event = new MidiEvent(s,tick);
        }catch (Exception ex){ex.printStackTrace();}

        return event;
    }

    public void saveFile(File file){
        boolean[] checkBoxState = new boolean[256];

        for (int i=0;i<256;i++) {
            JCheckBox checkBox = checkBoxList.get(i);

            checkBoxState[i] = checkBox.isSelected();
        }

        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileStream);
            outputStream.writeObject(checkBoxState);
            outputStream.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void loadFile(File file){
        boolean[] checkBoxState = null;

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            checkBoxState = (boolean []) (objectInputStream.readObject());
        }catch (Exception ex){
            ex.printStackTrace();
        }

        for (int i=0;i<256;i++){
            JCheckBox jc = checkBoxList.get(i);

            assert checkBoxState != null;
            jc.setSelected(checkBoxState[i]);
        }

        sequencer.stop();
    }
}

