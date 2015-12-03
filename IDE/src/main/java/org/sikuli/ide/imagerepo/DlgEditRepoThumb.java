/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide.imagerepo;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.max;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Mario
 */
public class DlgEditRepoThumb extends javax.swing.JDialog {
    String ATTR_NAME_TAGS = "Sikuli_ImageTags";
    final Path filePath;
    
    class TagsTableModel extends DefaultTableModel{

        @Override
        public void setValueAt(Object o, int i, int i1) {
            if (!o.toString().trim().equals("")){
                super.setValueAt(o, i, i1);
            }else{
                removeRow(i);
            }
        }
        
        public void addEmptyRow(){
            super.addRow(new String[]{""});
        }
        
        @Override
        public void addRow(Object[] os) {
            for (Object s:os){
                if (s.toString().trim().equals("")){
                    return;
                }
            }
            super.addRow(os);
        }
        
    }
    /**
     * Creates new form DlgEditRepoThumbb
     */
    public DlgEditRepoThumb(java.awt.Frame parent, boolean modal, Path filePath) {
        super(parent, modal);
        this.filePath = filePath;
        initComponents();
        
        this.lTextName.setText(filePath.getFileName().toString());
        this.lTextPath.setText("<html>"+filePath.toString()+"</html>");
        try {
            BufferedImage image = ImageIO.read(new File(filePath.toString()));
            float xRatio = (float)image.getWidth() / 200;
            float yRatio = (float)image.getHeight() / 100;
            float finalRatio = max(xRatio, yRatio);
            int width = (int)(image.getWidth() / finalRatio);
            int height = (int)(image.getHeight() / finalRatio);
            if (width == 0) width = 1;
            if (height == 0) height = 1;
            ImageIcon imageIcon = new ImageIcon(image.getScaledInstance(width, height, 0));
            this.imageLabel.setIcon(imageIcon);
            this.cbAlpha.setSelected(image.getColorModel().hasAlpha());
            this.tblTags.setModel(new TagsTableModel());
            this.readTagsAttr(filePath);
            this.tblTags.addKeyListener(new KeyListener(){
                @Override
                public void keyTyped(KeyEvent ke) {}
                @Override
                public void keyPressed(KeyEvent ke) {}
                @Override
                public void keyReleased(KeyEvent ke) {
                    if (ke.getKeyCode() == 127){;
                        TagsTableModel mdl = (TagsTableModel)tblTags.getModel();
                        for (int idx:tblTags.getSelectedRows()){
                            mdl.removeRow(idx);
                        }
                    }
                }
            });
        } catch (IOException ex) {
        }
        this.setSize(350, 350);
    }
    private void readTagsAttr(Path filePath){
        TagsTableModel mdl = (TagsTableModel)tblTags.getModel();
        mdl.addColumn(null);
        try {
            UserDefinedFileAttributeView attrs = Files.getFileAttributeView( filePath.toAbsolutePath(),
                                   UserDefinedFileAttributeView.class );
            ByteBuffer attrValue  = ByteBuffer.allocate(attrs.size("Sikuli_ImageTags"));
            attrs.read(ATTR_NAME_TAGS, attrValue);
            attrValue.rewind();
            String value = StandardCharsets.UTF_8.decode( attrValue ).toString();
            for (String tag:value.split(",")){
                //tags.add(tag);
                mdl.addRow(new String[]{tag});
            }
            //mdl.addColumn(null, tags.toArray());
        } catch (IOException ex) {
        }
    }
    
    private void writeTagsAttr(Path filePath, Iterable tags){
        String arrayValue = "";
        Iterator<String> iter = tags.iterator();
        arrayValue += iter.hasNext() ? iter.next() : "";
        while (iter.hasNext()) {
            arrayValue += "," + iter.next();
        }
        try {
            UserDefinedFileAttributeView attrs = Files.getFileAttributeView( filePath.toAbsolutePath(),
                                   UserDefinedFileAttributeView.class );
            attrs.write(ATTR_NAME_TAGS,
                        ByteBuffer.wrap( arrayValue.getBytes( StandardCharsets.UTF_8 ) ) );
           } catch (IOException ex) {
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lName = new javax.swing.JLabel();
        lPath = new javax.swing.JLabel();
        lTags = new javax.swing.JLabel();
        btnAddTag = new javax.swing.JButton();
        lTextName = new javax.swing.JLabel();
        lTextPath = new javax.swing.JLabel();
        imageLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        btnOk = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        cbAlpha = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblTags = new org.jdesktop.swingx.JXTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(300, 300));
        setModal(true);
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {0, 7, 0};
        layout.rowHeights = new int[] {0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0};
        getContentPane().setLayout(layout);

        lName.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(lName, gridBagConstraints);

        lPath.setText("Pfad:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(lPath, gridBagConstraints);

        lTags.setText("Tags:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(lTags, gridBagConstraints);

        btnAddTag.setText("+");
        btnAddTag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddTagActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(btnAddTag, gridBagConstraints);

        lTextName.setText("jLabel1");
        lTextName.setMaximumSize(new java.awt.Dimension(200, 14));
        lTextName.setMinimumSize(new java.awt.Dimension(60, 14));
        lTextName.setName(""); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        getContentPane().add(lTextName, gridBagConstraints);

        lTextPath.setText("jLabel2");
        lTextPath.setMinimumSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(lTextPath, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        getContentPane().add(imageLabel, gridBagConstraints);

        btnOk.setText("Ok");
        btnOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkActionPerformed(evt);
            }
        });
        jPanel1.add(btnOk);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        jPanel1.add(btnCancel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(jPanel1, gridBagConstraints);

        jLabel1.setText("Alpha:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 0, 0);
        getContentPane().add(jLabel1, gridBagConstraints);

        cbAlpha.setEnabled(false);
        cbAlpha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbAlphaActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(cbAlpha, gridBagConstraints);

        tblTags.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tblTags.setAutoStartEditOnKeyStroke(false);
        tblTags.setShowHorizontalLines(false);
        tblTags.setShowVerticalLines(false);
        tblTags.setTableHeader(null);
        jScrollPane1.setViewportView(tblTags);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 6);
        getContentPane().add(jScrollPane1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
        TagsTableModel mdl = (TagsTableModel)tblTags.getModel();
        ArrayList<String> tags = new ArrayList<String>();
        for (int i=0;i<mdl.getRowCount();i++){
            tags.add((String)mdl.getValueAt(i, 0));
        }
        writeTagsAttr(filePath, tags);
        dispose();
    }//GEN-LAST:event_btnOkActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void cbAlphaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbAlphaActionPerformed
    }//GEN-LAST:event_cbAlphaActionPerformed

    private void btnAddTagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddTagActionPerformed
        // stop current edit first
        if (tblTags.isEditing()){
           tblTags.getCellEditor().stopCellEditing();
        }
        // add an empty entry
        TagsTableModel mdl = (TagsTableModel)tblTags.getModel();
        mdl.addEmptyRow();
        int idx = mdl.getRowCount() - 1;
        
        // edit the new entry
        tblTags.editCellAt(idx, 0);
        tblTags.getEditorComponent().requestFocus();
    }//GEN-LAST:event_btnAddTagActionPerformed

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
            java.util.logging.Logger.getLogger(DlgEditRepoThumb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DlgEditRepoThumb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DlgEditRepoThumb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DlgEditRepoThumb.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                DlgEditRepoThumb dialog = new DlgEditRepoThumb(new javax.swing.JFrame(), true, Paths.get("C:\\tmp\\sikuli\\images\\Unbekannt.png"));
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddTag;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOk;
    private javax.swing.JCheckBox cbAlpha;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lName;
    private javax.swing.JLabel lPath;
    private javax.swing.JLabel lTags;
    private javax.swing.JLabel lTextName;
    private javax.swing.JLabel lTextPath;
    private org.jdesktop.swingx.JXTable tblTags;
    // End of variables declaration//GEN-END:variables
}
