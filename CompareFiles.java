/*
 * =============================================================================================
 * Name: CompareFiles.java
 * Desc: A Java Swing application for custom comparison of two csv files.
 *       Use the following invocation if heap size is not big enough:
 *           C:\> java -Xmx500m CompareFiles
 *       Use the following command to view the Java processes:
 *           C:\> jps -lvm
 *       This application has been tested in Java 5, 6 and 7 only.
 * By  : prat
 * On  : 12/1/2013
 * =============================================================================================
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

public class CompareFiles implements ActionListener {
    JPanel textPanel, panelForTextFields, completionPanel;
    JLabel titleLabel, file1Label, file2Label, keysLabel, toleranceLabel, excludeLabel;
    JTextField file1Field, file2Field, keysField, toleranceField, excludeField;
    JButton diffButton, closeButton;
    DefaultListModel sampleModel;

    JList resultList;
    JScrollPane scrollPanel;
    Hashtable<String, Boolean> keyHash, excludeHash;
    Hashtable<String, String[]> dataHash;
    String file1, file2;
    double tolerance;
    ArrayList<HeaderCol> header;
    ArrayList<ComparisonMetaData> cmpMetaData;
    JButton browseButton1, browseButton2, previewButton1, previewButton2, selectKeysButton, selectExcludeButton;
    JFileChooser jfc1, jfc2;

    // SearchLabel, textfield and button
    JLabel searchLabel;
    JTextField searchField;
    JButton searchButton;

    public JPanel createContentPane() {
        // We create a bottom JPanel to place everything on.
        JPanel totalGUI = new JPanel();
        totalGUI.setLayout(null);

        titleLabel = new JLabel("Comparison Details");
        titleLabel.setLocation(0,0);
        titleLabel.setSize(290, 30);
        titleLabel.setHorizontalAlignment(0);
        totalGUI.add(titleLabel);

        // Creation of a Panel to contain the JLabels
        textPanel = new JPanel();
        textPanel.setLayout(null);
        textPanel.setLocation(5, 40);
        textPanel.setSize(70, 200);
        totalGUI.add(textPanel);

        // File1 Label
        file1Label = new JLabel("File 1");
        file1Label.setLocation(0, 0);
        file1Label.setSize(70, 40);
        file1Label.setHorizontalAlignment(4);
        textPanel.add(file1Label);

        // File2 Label
        file2Label = new JLabel("File 2");
        file2Label.setLocation(0, 40);
        file2Label.setSize(70, 40);
        file2Label.setHorizontalAlignment(4);
        textPanel.add(file2Label);

        // Keys Label
        keysLabel = new JLabel("Keys");
        keysLabel.setLocation(0, 80);
        keysLabel.setSize(70, 40);
        keysLabel.setHorizontalAlignment(4);
        textPanel.add(keysLabel);

        // Tolerance Label
        toleranceLabel = new JLabel("Tolerance");
        toleranceLabel.setLocation(0, 120);
        toleranceLabel.setSize(70, 40);
        toleranceLabel.setHorizontalAlignment(4);
        textPanel.add(toleranceLabel);

        // Exclude Label
        excludeLabel = new JLabel("Exclude");
        excludeLabel.setLocation(0, 160);
        excludeLabel.setSize(70, 40);
        excludeLabel.setHorizontalAlignment(4);
        textPanel.add(excludeLabel);

        // TextFields Panel Container
        panelForTextFields = new JPanel();
        panelForTextFields.setLayout(null);
        panelForTextFields.setLocation(100, 40);
        panelForTextFields.setSize(200, 200);
        totalGUI.add(panelForTextFields);

        // File1 Textfield
        file1Field = new JTextField();
        file1Field.setLocation(0, 0);
        file1Field.setSize(200, 40);
        panelForTextFields.add(file1Field);

        // File2 Textfield
        file2Field = new JTextField();
        file2Field.setLocation(0, 40);
        file2Field.setSize(200, 40);
        panelForTextFields.add(file2Field);

        // Keys Textfield
        keysField = new JTextField();
        keysField.setLocation(0, 80);
        keysField.setSize(200, 40);
        panelForTextFields.add(keysField);

        // Tolerance Textfield
        toleranceField = new JTextField("0.01");
        toleranceField.setLocation(0, 120);
        toleranceField.setSize(200, 40);
        panelForTextFields.add(toleranceField);

        // Exclude Textfield
        excludeField = new JTextField();
        excludeField.setLocation(0, 160);
        excludeField.setSize(200, 40);
        panelForTextFields.add(excludeField);

        // Creation of a Panel to contain the completion JLabels
        completionPanel = new JPanel();
        completionPanel.setLayout(null);
        completionPanel.setLocation(305, 40);
        completionPanel.setSize(190, 200);
        totalGUI.add(completionPanel);

        // Browse and preview buttons for File1
        browseButton1 = new JButton("Browse");
        browseButton1.setLocation(0, 0);
        browseButton1.setSize(80, 35);
        browseButton1.addActionListener(this);
        completionPanel.add(browseButton1);

        previewButton1 = new JButton("Preview");
        previewButton1.setLocation(85, 0);
        previewButton1.setSize(90, 35);
        previewButton1.addActionListener(this);
        completionPanel.add(previewButton1);

        // Browse and preview buttons for File2
        browseButton2 = new JButton("Browse");
        browseButton2.setLocation(0, 40);
        browseButton2.setSize(80, 35);
        browseButton2.addActionListener(this);
        completionPanel.add(browseButton2);

        previewButton2 = new JButton("Preview");
        previewButton2.setLocation(85, 40);
        previewButton2.setSize(90, 35);
        previewButton2.addActionListener(this);
        completionPanel.add(previewButton2);

        selectKeysButton = new JButton("Select");
        selectKeysButton.setLocation(0, 80);
        selectKeysButton.setSize(80, 35);
        selectKeysButton.addActionListener(this);
        completionPanel.add(selectKeysButton);

        selectExcludeButton = new JButton("Select");
        selectExcludeButton.setLocation(0, 160);
        selectExcludeButton.setSize(80, 35);
        selectExcludeButton.addActionListener(this);
        completionPanel.add(selectExcludeButton);

        // Button for performing diff
        diffButton = new JButton("Diff");
        diffButton.setLocation(100, 250);
        diffButton.setSize(80, 30);
        diffButton.addActionListener(this);
        totalGUI.add(diffButton);

        resultList = new JList();
        scrollPanel = new JScrollPane(resultList);
        scrollPanel.setLocation(0, 300);
        scrollPanel.setSize(490, 100);
        totalGUI.add(scrollPanel);

        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = resultList.locationToIndex(e.getPoint());
                    try {
                        //displayDataTable (index);
                        DataTable dt = new DataTable(index, new String [1]);
                        dt.displayDataTable();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "File processing error", JOptionPane.ERROR_MESSAGE);
                    }
                 }
            }
        };
        resultList.addMouseListener(mouseListener);

        // SearchLabel, textfield and button
        searchLabel = new JLabel("Key");
        searchLabel.setLocation(50, 420);
        searchLabel.setSize(70, 40);
        totalGUI.add(searchLabel);

        searchField = new JTextField();
        searchField.setLocation(100, 420);
        searchField.setSize(200, 40);
        totalGUI.add(searchField);

        searchButton = new JButton("Search");
        searchButton.setLocation(305, 420);
        searchButton.setSize(80, 40);
        searchButton.addActionListener(this);
        totalGUI.add(searchButton);

        // Button for closing the app
        closeButton = new JButton("Close");
        closeButton.setLocation(100, 480);
        closeButton.setSize(80, 30);
        closeButton.addActionListener(this);
        totalGUI.add(closeButton);

        totalGUI.setOpaque(true);
        return totalGUI;
    }

    class DataTable {
        private Hashtable<String, Boolean> diffIndexes = new Hashtable<String, Boolean>();
        private Object[] columnNames;
        private Object[][] rowData;
        private int leftLineNumber;
        private int rightLineNumber;

        DataTable (int index, String[] keys) throws Exception {
            if (index >= 0) {
                ComparisonMetaData cmd = cmpMetaData.get(index);
                leftLineNumber = cmd.getLeftLineNumber();
                rightLineNumber = cmd.getRightLineNumber();

                // populate diffIndexes with the positions of columns that have differences
                if (leftLineNumber != -1 && rightLineNumber != -1) {
                    String message = sampleModel.get(index).toString();
                    message = message.substring(0, message.length() - 1);  // discard the newline at the end of the string
                    int fromIndex, toIndex, messageLength;
                    fromIndex = message.indexOf(":", message.indexOf("Mismatched columns", 0));
                    messageLength = message.length();
                    while (fromIndex <= messageLength) {
                        toIndex = message.indexOf(",", fromIndex);
                        if (toIndex == -1) {
                            int i = Integer.parseInt(message.substring(fromIndex + 1)) - 1;
                            diffIndexes.put(String.valueOf(i), true);
                            fromIndex = messageLength + 1;
                        } else {
                            int i = Integer.parseInt(message.substring(fromIndex + 1, toIndex)) - 1;
                            diffIndexes.put(String.valueOf(i), true);
                            fromIndex = message.indexOf(":", toIndex + 1);
                        }
                    }
                }
            }

            rowData = new Object[header.size()][4];
            for (int i = 0; i < header.size(); i++) {
                rowData[i][0] = i + 1;
                rowData[i][1] = header.get(i).getName();
            }

            // Populate rowData from the files, if needed
            populateRowData(file1, leftLineNumber, 2, keys);
            populateRowData(file2, rightLineNumber, 3, keys);

            columnNames = new Object[]{
                                           "Position",
                                           "Column Name",
                                           "File 1" + (leftLineNumber == -1 ? "" : " (" + leftLineNumber + ")"),
                                           "File 2" + (rightLineNumber == -1 ? "" : " (" + rightLineNumber + ")")
                                      };
        }

        void populateRowData(String file, int lineNo, int column, String[] keys) throws Exception {
            try {
                Boolean keysFound = false;
                String line;
                int lineNumber = 0;
                FileInputStream fis = new FileInputStream(file);
                Scanner scanner = new Scanner(fis);
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    lineNumber++;

                    if (lineNo == 0 && matchSuccess(line, keys)) {
                        keysFound = true;
                        if (column == 2) {
                            leftLineNumber = lineNumber;
                        } else if (column == 3) {
                            rightLineNumber = lineNumber;
                        }
                    }

                    if (lineNumber == lineNo || keysFound) {
                        // Parse this line, load rowData array and break
                        int fromInd, toInd, len, iter;
                        fromInd = 0;
                        len = line.length();
                        iter = 0;
                        while (fromInd <= len) {
                            if (iter >= header.size()) {  // data lines may have more tokens than header line, hence ignore
                                break;
                            }
                            toInd = line.indexOf(",", fromInd);
                            if (toInd == -1) {
                                rowData[iter][column] = line.substring(fromInd);
                                break;                                            // forceful termination
                            } else {
                                rowData[iter][column] = line.substring(fromInd, toInd);
                                fromInd = toInd + 1;
                            }
                            iter++;
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                throw new Exception("Unable to process file : " + file);
            }
        }

        Boolean matchSuccess (String line, String[] keys) {
            String[] arr = delimitedStringToArray(line, ",");
            int matches = 0;
            int keyCount = 0;
            int iter = 0;
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).getIsKey()) {
                    keyCount++;
                    if (iter <= keys.length - 1 && keys[iter].equals(arr[i])) {
                        matches++;
                    }
                    iter++;
                }
            }

            // If the three values:
            //     (1) size of the "keys" array,
            //     (2) the key count in the header array and
            //     (3) the match count
            // are all equal, then we have a match
            if (keyCount == keys.length && keyCount == matches) {
                return true;
            } else {
                return false;
            }
        }

        void displayDataTable() throws Exception {
            if (leftLineNumber == 0 && rightLineNumber == 0) {
                throw new Exception("The key was not found in either file.");
            }
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JTable table = new JTable(rowData, columnNames)
            {
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
                {
                    Component c = super.prepareRenderer(renderer, row, column);
                    if (!isRowSelected(row)) {
                        if (header.get(row).getIsKey())
                            c.setBackground(new Color(177,177,255)); // A shade of blue
                        else if (header.get(row).getIsExclude())
                            c.setBackground(Color.LIGHT_GRAY);
                        else if (diffIndexes.containsKey(String.valueOf(row)))
                            c.setBackground(new Color(255,145,71));  // Light orange-red
                        else
                            c.setBackground(getBackground());
                    }
                    return c;
                }
            };
            JScrollPane scrollPane = new JScrollPane(table);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.setSize(300, 150);
            frame.setVisible(true);
        }
    }

    // Validate input, load data structures, compare data and display results in JList
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == diffButton) {
            try {
                if (file1Field.getText().trim().compareTo("") == 0) {
                    throw new Exception("File1 field is either blank or invalid.");
                } else if (file2Field.getText().trim().compareTo("") == 0) {
                    throw new Exception("File2 field is either blank or invalid.");
                } else if (file1Field.getText().trim().equalsIgnoreCase(file2Field.getText().trim())) {
                    throw new Exception("File1 and File2 must be different.");
                } else if (keysField.getText().trim().compareTo("") == 0) {
                    throw new Exception("Keys field is either blank or invalid.");
                }

                if (toleranceField.getText().trim().compareTo("") == 0) {
                    tolerance = 0.01;             // default value
                } else if (! isNumber(toleranceField.getText().trim())) {
                    throw new Exception("Tolerance field is not numeric.");
                } else {
                    tolerance = Double.parseDouble (toleranceField.getText().trim());
                }

                keyHash = new Hashtable<String, Boolean>();
                populateHash (keysField.getText(), keyHash, "key");

                excludeHash = new Hashtable<String, Boolean>();
                if (excludeField.getText().trim().compareTo("") != 0) {
                    populateHash (excludeField.getText(), excludeHash, "exclude column");

                    // Balk if key and exclude columns overlap
                    Enumeration excludeCols = excludeHash.keys();
                    String excludeKey;
                    while (excludeCols.hasMoreElements()) {
                        excludeKey = (String) excludeCols.nextElement();
                        if (keyHash.containsKey(excludeKey)) {
                            throw new Exception("Key and exclude columns overlap.");
                        }
                    }
                }

                file1 = file1Field.getText().trim();
                File f1 = new File(file1);
                if (! f1.isFile() || ! f1.canRead()) {
                    throw new Exception("Either File1 does not exist or it cannot be read.");
                }

                file2 = file2Field.getText().trim();
                File f2 = new File(file2);
                if (! f2.isFile() || ! f2.canRead()) {
                    throw new Exception("Either File2 does not exist or it cannot be read.");
                }

                readHeader(file1);

                compareHeaders(file2);
                loadData(file1);

                sampleModel = new DefaultListModel();
                compareData(file2);

                resultList.setModel(sampleModel);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Data entry error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == browseButton1) {
            jfc1 = new JFileChooser();
            int retval1 = jfc1.showOpenDialog(null);
            if (retval1 == JFileChooser.APPROVE_OPTION) {
                File theFile = jfc1.getSelectedFile();
                if (theFile != null) {
                    file1Field.setText(theFile.getPath());
                }
            }
        } else if (e.getSource() == browseButton2) {
            jfc2 = new JFileChooser();
            int retval2 = jfc2.showOpenDialog(null);
            if (retval2 == JFileChooser.APPROVE_OPTION) {
                File theFile = jfc2.getSelectedFile();
                if (theFile != null) {
                    file2Field.setText(theFile.getPath());
                }
            }
        } else if (e.getSource() == previewButton1) {
            if (file1Field.getText().trim().compareTo("") != 0) {
                try {
                    previewFile(file1Field.getText().trim());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Preview error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (e.getSource() == previewButton2) {
            if (file2Field.getText().trim().compareTo("") != 0) {
                try {
                    previewFile(file2Field.getText().trim());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Preview error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (e.getSource() == selectKeysButton) {
            try {
                KeysExcludeSelection kes = new KeysExcludeSelection( file1Field.getText().trim(),
                                                                     file2Field.getText().trim(),
                                                                     keysField.getText().trim(),
                                                                     excludeField.getText().trim(),
                                                                     "Choose Key Columns"
                                                                   );
                kes.displayColumnChoices(keysField);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Data entry error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == selectExcludeButton) {
            try {
                KeysExcludeSelection kes = new KeysExcludeSelection( file1Field.getText().trim(),
                                                                     file2Field.getText().trim(),
                                                                     excludeField.getText().trim(),
                                                                     keysField.getText().trim(),
                                                                     "Choose Exclude Columns"
                                                                   );
                kes.displayColumnChoices(excludeField);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Data entry error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == searchButton) {
            searchForKeys(searchField.getText().trim());
        } else if (e.getSource() == closeButton) {
            System.exit(0);
        }
    }

    void searchForKeys (String strKeys) {
        if (strKeys == null || strKeys.length() == 0 || sampleModel == null || sampleModel.size() == 0) {
            return;
        }

        String[] columns = delimitedStringToArray(strKeys, ",");
        int index = -1;
        String key;
        for (int i = 0; i < sampleModel.size(); i++) {
            key = sampleModel.get(i).toString();
            key = key.replace("KEY: (", "");
            key = key.substring(0, key.indexOf(")"));
            if (strKeys.equals(key)) {
                index = i;
                resultList.setSelectedIndex(i);
                resultList.ensureIndexIsVisible(i);
                break;
            }
        }

        try {
            DataTable dt = new DataTable(index, columns);
            dt.displayDataTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "File processing error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String[] delimitedStringToArray (String str, String delim) {
        // ==========================================================================================================
        // I wrote this method because I was not satisfied with the way String.split() and StringTokenizer work.
        // The string ",abc,,ghi,," when split on comma "," should return an array of length 6 and contents:
        // 0 => ""
        // 1 => "abc"
        // 2 => ""
        // 3 => "ghi"
        // 4 => ""
        // 5 => ""
        // ==========================================================================================================
        int len = str.length();
        ArrayList<String> arrList = new ArrayList<String>();

        if (str.indexOf(delim) == -1) {
            // No delimiter here
            arrList.add(str);
        } else {
            int fromInd = 0;
            int toInd;
            while (fromInd <= len) {
                toInd = str.indexOf(delim, fromInd);
                if (fromInd == toInd) {
                    // Found a zero length token
                    arrList.add("");
                    fromInd = toInd + delim.length();
                } else if (toInd == -1) {
                    // Load final token and break
                    arrList.add(str.substring(fromInd));
                    break;
                } else {
                    arrList.add(str.substring(fromInd, toInd));
                    fromInd = toInd + delim.length();
                }
            }
        }
        // convert ArrayList to String[]
        String[] arr = arrList.toArray(new String[arrList.size()]);
        return arr;
    }

    class KeysExcludeSelection {
        private String frameTitle;
        private DefaultListModel columnListModel;
        private int[] selIndices;

        KeysExcludeSelection (String strFile1, String strFile2, String thisColumnList, String otherColumnList, String title) throws Exception {
            frameTitle = title;

            if (strFile1.compareTo("") == 0) {
                throw new Exception("File1 field is either blank or invalid.");
            } else if (strFile2.compareTo("") == 0) {
                throw new Exception("File2 field is either blank or invalid.");
            } else if (strFile1.equalsIgnoreCase(strFile2)) {
                throw new Exception("File1 and File2 must be different.");
            }
            File f1 = new File(strFile1);
            if (! f1.isFile() || ! f1.canRead()) {
                throw new Exception("Either File1 does not exist or it cannot be read.");
            }
            File f2 = new File(strFile2);
            if (! f2.isFile() || ! f2.canRead()) {
                throw new Exception("Either File2 does not exist or it cannot be read.");
            }

            FileInputStream fis1 = new FileInputStream(strFile1);
            Scanner scanner1 = new Scanner(fis1);
            if (! scanner1.hasNextLine()) {
                throw new Exception("No lines in file: " + strFile1);
            }
            String str = scanner1.nextLine().trim();
            if (str.startsWith(",") || str.endsWith(",")) {
                throw new Exception("Null columns in header of : " + strFile1);
            }

            FileInputStream fis2 = new FileInputStream(strFile2);
            Scanner scanner2 = new Scanner(fis2);
            if (! scanner2.hasNextLine()) {
                throw new Exception("No lines in file: " + strFile2);
            }
            if (! str.equalsIgnoreCase(scanner2.nextLine().trim())) {            // Poor man's header comparison
                throw new Exception("Headers in the two files do not match.");
            }

            // If the "Key Select" button is pressed, then: "this" textbox => KeysField; "other" textbos => ExcludeField
            // If the "Exclude Select" button is pressed, then: "this" textbox => ExcludeField; "other" textbos => KeysField

            // Populate the hashtable with columns in "this" textbox
            Hashtable<String, Boolean> thisCol = new Hashtable<String, Boolean>();
            if (thisColumnList.length() > 0) {
                String[] cols = thisColumnList.split(",");
                for (int i = 0; i < cols.length; i++) {
                    thisCol.put(cols[i], true);
                }
            }

            // Populate the hashtable with columns in "other" textbox
            Hashtable<String, Boolean> otherCol = new Hashtable<String, Boolean>();
            if (otherColumnList.length() > 0) {
                String[] columns = otherColumnList.split(",");
                for (int i = 0; i < columns.length; i++) {
                    otherCol.put(columns[i], true);
                }
            }

            if (thisColumnList.length() > 0 && otherColumnList.length() > 0) {
                Enumeration enumOtherCol = otherCol.keys();
                String otherKey;
                while (enumOtherCol.hasMoreElements()) {
                    otherKey = (String) enumOtherCol.nextElement();
                    if (thisCol.containsKey(otherKey)) {
                        throw new Exception("Key and exclude columns overlap.");
                    }
                }
            }

            // Now populate the DefaultListModel with columns other than that in otherCol Hashtable
            ArrayList<Integer> listIndices = new ArrayList<Integer>();
            columnListModel = new DefaultListModel();
            int len = str.length();
            if (str.indexOf(",") == -1) {
                if (! otherCol.containsKey(str)) {
                    columnListModel.addElement(str);
                    if (thisCol.containsKey(str)) {
                        listIndices.add(new Integer(columnListModel.size() - 1));
                        //selIndices[iter] = columnListModel.size() - 1;
                    }
                }
            } else {
                int fromInd = 0;
                int toInd;
                while (fromInd <= len) {
                    toInd = str.indexOf(",", fromInd);
                    // Balk if any key is null or consists of blank spaces
                    if (fromInd == toInd) {
                        throw new Exception("Null columns in header of : " + strFile1);
                    } else if (toInd == -1) {
                        // Load final token and break
                        if (! otherCol.containsKey(str.substring(fromInd))) {
                            columnListModel.addElement(str.substring(fromInd));
                            if (thisCol.containsKey(str.substring(fromInd))) {
                                  listIndices.add(new Integer(columnListModel.size() - 1));
                            }
                        }
                        break;
                    } else {
                        if (str.substring(fromInd, toInd).trim().compareTo("") == 0) {
                            throw new Exception("Null columns in header of : " + strFile1);
                        }
                        if (! otherCol.containsKey(str.substring(fromInd, toInd))) {
                            columnListModel.addElement(str.substring(fromInd, toInd));
                            if (thisCol.containsKey(str.substring(fromInd, toInd))) {
                                  listIndices.add(new Integer(columnListModel.size() - 1));
                            }
                        }
                        fromInd = toInd + 1;
                    }
                }
            }

            // Convert ArrayList of Integer objects to priviate int array
            selIndices = new int[listIndices.size()];
            int i = 0;
            for (Integer n : listIndices) {
                selIndices[i++] = n;
            }
        }

        public void displayColumnChoices(final JTextField tf) {
            final JList columnList = new JList();
            columnList.setModel(columnListModel);
            columnList.setSelectedIndices(selIndices);

            JLabel columnLabel = new JLabel("Column List");
            columnLabel.setLocation(50, 10);
            columnLabel.setSize(80, 30);

            JScrollPane scrollPane = new JScrollPane(columnList);
            scrollPane.setLocation(50, 40);
            scrollPane.setSize(200, 300);

            JButton doneButton = new JButton("Done");
            doneButton.setLocation(50, 360);
            doneButton.setSize(80, 30);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.setLocation(170, 360);
            cancelButton.setSize(80, 30);

            JFrame.setDefaultLookAndFeelDecorated(true);
            final JFrame frame = new JFrame(frameTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(null);
            frame.add(columnLabel);
            frame.add(scrollPane);
            frame.add(doneButton);
            frame.add(cancelButton);
            frame.setSize(300, 450);
            frame.setVisible(true);

            cancelButton.addActionListener (new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    frame.dispose();
                }
            });

            doneButton.addActionListener (new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String finalList = "";
                    for (int i = 0; i < columnList.getSelectedValues().length; i++) {
                        finalList += "," + (columnList.getSelectedValues())[i];
                    }
                    if (finalList.length() != 0) {
                        tf.setText(finalList.substring(1));
                    }
                    frame.dispose();
                }
            });
        }
    }

    public void previewFile (String strFile) throws Exception {
        File file = new File(strFile);
        if (! file.isFile() || ! file.canRead()) {
            throw new Exception("Either file does not exist or it cannot be read.");
        }
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JTextArea ta = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(ta);
        FileInputStream fis = new FileInputStream(file);
        Scanner scanner = new Scanner(fis);
        int lineNumber = 0;
        int linesForPreview = 100;
        String line;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            lineNumber++;
            if (lineNumber >= linesForPreview) {
                break;
            }
            ta.append(line + "\n");
        }
        ta.setCaretPosition(0);
        ta.setEditable(false);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setSize(500,250);
        frame.setVisible(true);
    }

    public void loadData (String strFile) throws Exception {
        String line, key, token;
        boolean isFirstLine = true;
        int fromInd, toInd, len, iter;
        dataHash = new Hashtable<String, String[]>();
        String[] data;
        int lineNumber = 0;

        FileInputStream fis = new FileInputStream(strFile);
        Scanner scanner = new Scanner(fis);
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            lineNumber++;
            // Skip the header line
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            fromInd = 0;
            len = line.length();
            iter = 0;
            key = "";
            data = new String[header.size() + 1];  // header cols + last col has the line number
            while (fromInd <= len) {
                if (iter >= header.size()) {
                    break;
                }
                toInd = line.indexOf(",", fromInd);

                if (toInd == -1) {
                    token = line.substring(fromInd);
                    fromInd = len + 1;
                } else {
                    token = line.substring(fromInd, toInd);
                    fromInd = toInd + 1;
                }

                if (header.get(iter).getIsKey()) {
                    // this is a key value, append to key string and add null string to data array
                    key += "," + token;
                    data[iter] = "";
                } else if (header.get(iter).getIsExclude()) {
                    // this is an exclude value, add null string to data array
                    data[iter] = "";
                } else {
                    // neither key nor exclude column, add to data array
                    data[iter] = token;
                }

                iter++;
            }
            key = key.substring(1);
            data[iter] = String.valueOf(lineNumber);
            dataHash.put(key, data);
        }
    }

    public void compareData (String strFile) throws Exception {
        String line, key, token;
        boolean isFirstLine = true;
        int fromInd, toInd, len, iter;
        String[] data;
        int lineNumber = 0;
        cmpMetaData = new ArrayList<ComparisonMetaData>();

        FileInputStream fis = new FileInputStream(strFile);
        Scanner scanner = new Scanner(fis);
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            lineNumber++;
            // Skip the header line
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            fromInd = 0;
            len = line.length();
            iter = 0;
            key = "";
            data = new String[header.size() + 1];
            while (fromInd <= len) {
                if (iter >= header.size()) {
                    break;
                }
                toInd = line.indexOf(",", fromInd);

                if (toInd == -1) {
                    token = line.substring(fromInd);
                    fromInd = len + 1;
                } else {
                    token = line.substring(fromInd, toInd);
                    fromInd = toInd + 1;
                }

                if (header.get(iter).getIsKey()) {
                    // this is a key value, append to key string and add null string to data array
                    key += "," + token;
                    data[iter] = "";
                } else if (header.get(iter).getIsExclude()) {
                    // this is an exclude value, add null string to data array
                    data[iter] = "";
                } else {
                    // neither key nor exclude column, add to data array
                    data[iter] = token;
                }

                iter++;
            }
            key = key.substring(1);
            data[iter] = String.valueOf(lineNumber);
            // if the key does not exist in dataHash, then print message
            // otherwise compare their value arrays
            if (dataHash.containsKey(key)) {
                String mismatches = findMismatches (data, dataHash.get(key));
                if (mismatches.compareTo("") != 0) {
                    String[] dataArray = dataHash.get(key);
                    int leftLineNum = Integer.parseInt(dataArray[dataArray.length-1]);
                    // Add to main ArrayList
                    ComparisonMetaData cmp = new ComparisonMetaData(leftLineNum, lineNumber);
                    cmpMetaData.add(cmp);
                    sampleModel.addElement(String.format ("KEY: %-30s => Mismatched columns = %s\n", "("+key+")", mismatches));
                }
                // remove this key from the dataHash, since comparison is done
                dataHash.remove(key);
            } else {
                ComparisonMetaData cmp = new ComparisonMetaData(-1, lineNumber);
                cmpMetaData.add(cmp);
                sampleModel.addElement(String.format ("KEY: %-30s => Found only in %s\n", "("+key+")", strFile));
            }
        }
        // The remaining keys in dataHash are the ones found only in the first file
        Enumeration dataEnum = dataHash.keys();
        String dataKey;
        String[] arr;
        int leftLine;
        while (dataEnum.hasMoreElements()) {
            dataKey = (String) dataEnum.nextElement();
            arr = (String[])dataHash.get(dataKey);
            leftLine = Integer.parseInt(arr[arr.length-1]);
            ComparisonMetaData cmp = new ComparisonMetaData(leftLine, -1);
            cmpMetaData.add(cmp);
            sampleModel.addElement(String.format ("KEY: %-30s => Found only in %s\n", "("+dataKey+")", file1));
            dataHash.remove(dataKey);
        }
    }

    public String findMismatches (String[] arr1, String[] arr2) throws Exception {
        String columnList = "";
        for (int i = 0; i < arr1.length - 1; i++) {
            if (areUnequal(arr1[i], arr2[i])) {
                columnList += "," + header.get(i).getName() + ":" + (i + 1);
            }
        }
        if (columnList.compareTo("") != 0) {
            columnList = columnList.substring(1);
        }
        return columnList;
    }

    public boolean areUnequal (String value1, String value2) {
        boolean comparisonOutcome;
        if (isNumber(value1) && isNumber(value2)) {
            comparisonOutcome = Math.abs(Math.abs(Double.parseDouble(value1)) - Math.abs(Double.parseDouble(value2))) >= tolerance;
        } else {
            comparisonOutcome = !value1.equals(value2);
        }
        return comparisonOutcome;
    }

    public void readHeader (String strFile) throws Exception {
        header = new ArrayList<HeaderCol>();
        FileInputStream fis = new FileInputStream(strFile);
        Scanner scanner = new Scanner(fis);

        if (! scanner.hasNextLine()) {
            throw new Exception("No lines in file: " + strFile);
        }

        String str = scanner.nextLine().trim();
        int len = str.length();

        if (str.startsWith(",") || str.endsWith(",")) {
            throw new Exception("Null columns in header of : " + strFile);
        } else if (str.indexOf(",") == -1) {
            HeaderCol hc = new HeaderCol(str, false, false);
            if (keyHash.containsKey(str)) {
                hc.setIsKey(true);
                keyHash.put(str, true);
            } else if (excludeHash.containsKey(str)) {
                hc.setIsExclude(true);
                excludeHash.put(str, true);
            }
            header.add(hc);
        } else {
            int fromInd = 0;
            int toInd;
            while (fromInd <= len) {
                toInd = str.indexOf(",", fromInd);
                // Balk if any key is null or consists of blank spaces
                if (fromInd == toInd) {
                    throw new Exception("Null columns in header of : " + strFile);
                } else if (toInd == -1) {
                    // Load final token and break
                    HeaderCol hc = new HeaderCol(str.substring(fromInd), false, false);
                    if (keyHash.containsKey(str.substring(fromInd))) {
                        hc.setIsKey(true);
                        keyHash.put(str.substring(fromInd), true);
                    } else if (excludeHash.containsKey(str.substring(fromInd))) {
                        hc.setIsExclude(true);
                        excludeHash.put(str.substring(fromInd), true);
                    }
                    header.add(hc);
                    break;
                } else {
                    if (str.substring(fromInd, toInd).trim().compareTo("") == 0) {
                        throw new Exception("Null columns in header of : " + strFile);
                    }
                    HeaderCol hc = new HeaderCol(str.substring(fromInd, toInd), false, false);
                    if (keyHash.containsKey(str.substring(fromInd, toInd))) {
                        hc.setIsKey(true);
                        keyHash.put(str.substring(fromInd, toInd), true);
                    } else if (excludeHash.containsKey(str.substring(fromInd, toInd))) {
                        hc.setIsExclude(true);
                        excludeHash.put(str.substring(fromInd, toInd), true);
                    }
                    header.add(hc);
                    fromInd = toInd + 1;
                }
            }
        }

        // Balk if any key or exclude columns were not found in the header
        Enumeration keyCols = keyHash.keys();
        String key;
        while (keyCols.hasMoreElements()) {
            key = (String) keyCols.nextElement();
            if ((Boolean)keyHash.get(key) == false) {
                throw new Exception("Key column " + key + " not found in header.");
            }
        }

        Enumeration excludeCols = excludeHash.keys();
        String excludeKey;
        while (excludeCols.hasMoreElements()) {
            excludeKey = (String) excludeCols.nextElement();
            if ((Boolean)excludeHash.get(excludeKey) == false) {
                throw new Exception("Exclude column " + excludeKey + " not found in header.");
            }
        }

    }

    public void compareHeaders (String strFile) throws Exception {
        int iter = 0;
        FileInputStream fis = new FileInputStream(strFile);
        Scanner scanner = new Scanner(fis);

        if (! scanner.hasNextLine()) {
            throw new Exception("No lines in file: " + strFile);
        }

        String str = scanner.nextLine().trim();
        int len = str.length();

        if (str.startsWith(",") || str.endsWith(",")) {
            throw new Exception("Null columns in header of : " + strFile);
        } else if (str.indexOf(",") == -1) {
            if (header.get(iter).getName().compareTo(str) != 0) {
                throw new Exception("Headers of the two files do not match.");
            }
        } else {
            int fromInd = 0;
            int toInd;
            while (fromInd <= len) {
                if (iter >= header.size()) {   // Header of File2 is longer than File1 and all tokens of File1 match with those of File2
                    throw new Exception("Headers of the two files do not match.");
                }

                toInd = str.indexOf(",", fromInd);
                // Balk if any key is null or consists of blank spaces
                if (fromInd == toInd) {
                    throw new Exception("Null columns in header of : " + strFile);
                } else if (toInd == -1) {
                    // Compare final token and break
                    if (header.get(iter).getName().compareTo(str.substring(fromInd)) != 0) {
                        throw new Exception("Headers of the two files do not match.");
                    }
                    break;
                } else {
                    if (str.substring(fromInd, toInd).trim().compareTo("") == 0) {
                        throw new Exception("Null columns in header of : " + strFile);
                    }
                    // Compare current token with the corresponding element name in header ArrayList
                    if (header.get(iter).getName().compareTo(str.substring(fromInd, toInd)) != 0) {
                        throw new Exception("Headers of the two files do not match.");
                    }
                    fromInd = toInd + 1;
                }
                iter++;
            }
        }
        if (iter < header.size() - 1) {   // Header of File2 is shorter than File1 and all tokens of File2 match with those of File1
            throw new Exception("Headers of the two files do not match.");
        }
    }

    class HeaderCol {
        private String name;
        private boolean isKey;
        private boolean isExclude;
        HeaderCol (String n, boolean k, boolean ex) {
            name = n;
            isKey = k;
            isExclude = ex;
        }
        void setIsKey(boolean k) { isKey = k; }
        void setIsExclude(boolean ex) { isExclude = ex; }
        String getName()  { return name; }
        boolean getIsKey() { return isKey; }
        boolean getIsExclude() { return isExclude; }
    }

    class ComparisonMetaData {
        private int leftLineNumber;
        private int rightLineNumber;
        ComparisonMetaData (int left, int right) {
            leftLineNumber = left;
            rightLineNumber = right;
        }
        void setLeftLineNumber(int lineNumber) { leftLineNumber = lineNumber; }
        void setRightLineNumber(int lineNumber) { rightLineNumber = lineNumber; }
        int getLeftLineNumber() { return leftLineNumber; }
        int getRightLineNumber() { return rightLineNumber; }
    }

    public boolean isNumber (String o) {
        boolean isNum = false;
        try {
            Double.valueOf(o);
            isNum = true;
        } catch (NumberFormatException e) {
            isNum = false;
        }
        return isNum;
    }

    public void populateHash (String str, Hashtable<String, Boolean> hash, String strType) throws Exception {
        // Populate the keys and exclude hashes
        String strList = str.trim();
        int strLen = strList.length();

        if (strList.startsWith(",") || strList.endsWith(",")) {
            throw new Exception("Null " + strType + " entered.");
        } else if (strList.indexOf(",") == -1) {
            hash.put(strList, false);
        } else {
            int fromInd = 0;
            int toInd;
            while (fromInd <= strLen) {
                toInd = strList.indexOf(",", fromInd);
                // Balk if any key is null or consists of blank spaces
                if (fromInd == toInd) {
                    throw new Exception("Null " + strType + " entered.");
                } else if (toInd == -1) {
                    // Load final token and break
                    hash.put(strList.substring(fromInd), false);
                    break;
                } else {
                    if (strList.substring(fromInd, toInd).trim().compareTo("") == 0) {
                        throw new Exception("Null " + strType + " entered.");
                    }
                    hash.put(strList.substring(fromInd, toInd), false);
                    fromInd = toInd + 1;
                }
            }
        }
    }

    private static void createAndShowGUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("Compare Files");
        CompareFiles demo = new CompareFiles();
        frame.setContentPane(demo.createContentPane());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 560);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}

