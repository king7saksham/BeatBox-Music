import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;

public class BeatBox {
    JPanel mainPanel;
    ArrayList<JCheckBox> checkBoxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentName = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Clap", "Hi Bongo", "Marcas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-Mid Tom", "High Agogo", "Open Hi Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {
        new BeatBox().buildGUI();
    }

    public void buildGUI(){
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkBoxList = new ArrayList<JCheckBox>();
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
        menu.add(clearMenuItem);
        menuBar.add(menu);

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

        setUpMIDI();

        theFrame.setJMenuBar(menuBar);
        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
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
        int[] trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i=0;i<16;i++){
            trackList = new int[16];

            int key = instruments[i];

            for (int j=0;j<16;j++){
                JCheckBox jc = (JCheckBox) checkBoxList.get(j+(16*i));
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

    public class MyClearListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            for(int i=0;i<256;i++){
                JCheckBox jCheckBox = (JCheckBox) (checkBoxList.get(i));
                jCheckBox.setSelected(false);
            }
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

    public void saveFile(File file){
        boolean[] checkBoxState = new boolean[256];

        for (int i=0;i<256;i++) {
            JCheckBox checkBox = (JCheckBox) (checkBoxList.get(i));

            if (checkBox.isSelected()) {
                checkBoxState[i] = true;
            } else {
                checkBoxState[i] = false;
            }
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
            FileInputStream fileInputStream = new FileInputStream("CheckBox.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            checkBoxState = (boolean []) (objectInputStream.readObject());
        }catch (Exception ex){
            ex.printStackTrace();
        }

        for (int i=0;i<256;i++){
            JCheckBox jc = (JCheckBox) (checkBoxList.get(i));

            jc.setSelected(checkBoxState[i]);
        }

        sequencer.stop();
    }
}
