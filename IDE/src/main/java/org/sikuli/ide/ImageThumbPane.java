/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.round;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import org.sikuli.basics.FileManager;
import org.sikuli.basics.PreferencesUser;
import org.sikuli.basics.Settings;
import org.sikuli.ide.imagerepo.DlgEditRepoThumb;
import org.sikuli.ide.imagerepo.FileTree;

/**
 * Copyright 2010-2014, Sikuli.org
 * Released under the MIT License.
 * 
 * @author MaSch 2014
 */
public class ImageThumbPane extends javax.swing.JPanel {
    private int rows;
    private int cols;
    private Path currentDir;
    private int[] selection; 
    private FileTree fileTree;
    
    public int getThumbHeight(){
        return ImageThumb.maxSize;
    }

   
    
    private class ThumbMouseListener implements MouseListener{
        private final ImageThumbPane pane;
        public ThumbMouseListener(ImageThumbPane pane){
            this.pane = pane;
        }
        @Override
        public void mouseClicked(MouseEvent me) {}

        @Override
        public void mousePressed(MouseEvent me) {
            pane.onMousePressed(me);
        }

        @Override
        public void mouseReleased(MouseEvent me) {}

        @Override
        public void mouseEntered(MouseEvent me) {}

        @Override
        public void mouseExited(MouseEvent me) {}
        
    }
    private class ImageThumb extends JPanel{
        private BufferedImage image;
        private BufferedImage scaledImg;
        private final int maxWidth = 120;
        private final int maxHeight = 110;
        public static final int maxSize = 130;
        private int width;
        private int height;
        private int dndWidth;
        private int dndHeight;
        private final String name;
        private Path imgPath;
        private boolean selected = false;
        
        public ImageThumb(String path) {
            imgPath = Paths.get(path);
            name = imgPath.getFileName().toString();
            this.setPreferredSize(new Dimension(130, 130));
            this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            try {                
               image = ImageIO.read(new File(path));
            }catch (IOException ex) {
                return;
            }
            this.setToolTipText("<html>" + name + "<br/>" + 
                       String.format("Size: %dx%d px ", image.getWidth(),image.getHeight()) + "</html>");
            float xRatio = (float)image.getWidth() / maxWidth;
            float yRatio = (float)image.getHeight() / maxHeight;
            float finalRatio = max(xRatio, yRatio);
            width = (int)(image.getWidth() / finalRatio);
            height = (int)(image.getHeight() / finalRatio);
            dndWidth = (int)(image.getWidth() / finalRatio*80/maxWidth);
            dndHeight = (int)(image.getHeight() / finalRatio*80/maxHeight);
            if (width == 0) width = 1;
            if (height == 0) height = 1;
            if (dndWidth == 0) dndWidth = 1;
            if (dndHeight == 0) dndHeight = 1;

            // Add Drag Support
            DragSource ds = new DragSource();
            ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, new DragGestureListener() {
                @Override
                public void dragGestureRecognized(DragGestureEvent event) {
                    String path = "\""+Settings.PROTOCOL_IMAGEREPO+Paths.get(PreferencesUser.getInstance().getPrefImageRepoPath()).relativize(imgPath)+"\"";
                    path = FileManager.slashify(path, false);
                    event.startDrag(null, image.getScaledInstance(dndWidth, dndHeight, 0), new Point(0,0), new ImageStringSelection(path, false), null);
                }
            });
            
            // Add Context Menu
            //JMenuItem menu = new JMenuItem("Rename");
            JMenuItem menuCopy = new JMenuItem("Copy");
            JMenuItem menuPaste = new JMenuItem("Paste");
            JMenuItem menuEdit = new JMenuItem("Edit Data");
            JMenuItem menuExplorer = new JMenuItem("Open in Explorer");
            JPopupMenu menuPopup = new JPopupMenu();
            menuPopup.add(menuCopy);
            menuPopup.add(menuPaste);
            menuPopup.add(menuEdit);
            menuPopup.addSeparator();
            menuPopup.add(menuExplorer);
            menuCopy.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    ImageThumb thumb = (ImageThumb)((JPopupMenu)((JMenuItem)ae.getSource()).getParent()).getInvoker();
                    Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    systemClipboard.setContents(new ImageStringSelection(thumb.getImagePath().toString(), false), null);
                }
            });
            menuEdit.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    ImageThumb thumb = (ImageThumb)((JPopupMenu)((JMenuItem)ae.getSource()).getParent()).getInvoker();
                    SikuliIDE ide = (SikuliIDE)thumb.getTopLevelAncestor();
                    DlgEditRepoThumb dlg = new DlgEditRepoThumb(ide, true, thumb.getImagePath());
                    dlg.setLocationRelativeTo(ide);
                    dlg.setVisible(true);
                }
                
            });
            menuExplorer.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    Desktop d =Desktop.getDesktop();
                    ImageThumb thumb = (ImageThumb)((JPopupMenu)((JMenuItem)ae.getSource()).getParent()).getInvoker();
                    Path path = thumb.getImagePath();
                    try {
                        d.browse(path.getParent().toUri());
                    } catch (IOException ex) {}
                }
            });
            
            this.setComponentPopupMenu(menuPopup);
        }
        
        public Path getImagePath(){
            return imgPath;
        }
        
        
        public void setSelected(boolean selected){
            this.selected = selected;
            this.repaint();
        }
        public boolean isSelected(){
            return selected;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // image
            int imgXPos = (int)(maxWidth - width) / 2;
            int imgYPos = (int)(maxHeight - height) / 2;
            g.drawImage(image.getScaledInstance(width, height, 0), imgXPos, imgYPos+2, null); // see javadoc for more info on the parameters
            
            if (selected){
                g.setColor(Color.BLUE);
            }
            // Border1
            if (height>10 && width > 10) g.draw3DRect(imgXPos, imgYPos+2, width, height, true);
            //Border2
            if (selected){
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(4));
                g.drawRect(2, 2, maxSize-4, maxSize-4);

                g2.setStroke(new BasicStroke(1));
                g.setColor(Color.BLACK);
            }
            // Text
            int stringLength = g.getFontMetrics().stringWidth(name);
            if (stringLength > maxWidth-10){
                float factor = (float) stringLength / (maxWidth-20);
                int numChars = (int) round(name.length() / factor);
                String shortName = name.substring(0, numChars-2) + "...";
                int xpos = (int)(maxWidth - g.getFontMetrics().stringWidth(shortName))/2;
                g.drawString(shortName, xpos, 124);
            }else{
                int xpos = (int)(maxWidth - g.getFontMetrics().stringWidth(name))/2;
                g.drawString(name, xpos, 124);
            }
        }
    }

    @Override
    public void reshape(int i, int i1, int i2, int i3) {
        super.reshape(i, i1, i2, i3);
        this.setPreferredSize(this.getSize());
        doLayout();
        repaint();
    }

    @Override
    public void setPreferredSize(Dimension dmnsn) {
        int width = dmnsn.width;
        dmnsn.height = calcPreferredHeight(width);
        super.setPreferredSize(dmnsn);
    }
    public int calcPreferredHeight(int width){
        int cols = width / ImageThumb.maxSize;
        int rows = this.getComponentCount() / cols;
        rows += getComponentCount() - cols*rows;
        return rows * ImageThumb.maxSize;
    }
    
    
    private class FileTreeSelectionListener implements TreeSelectionListener{
        private final ImageThumbPane thumbPane;
        public FileTreeSelectionListener(ImageThumbPane pane){
            thumbPane = pane;
        }
        @Override    
        public void valueChanged(TreeSelectionEvent tse) {
            thumbPane.clear();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof String){
                // Not valid data in the tree; it has to be of type Path.
                return;
            }
            Path directory = (Path)userObj; 
            currentDir = directory;
            thumbPane.scan(directory);
            thumbPane.setPreferredSize(thumbPane.getPreferredSize());
            thumbPane.getParent().doLayout();
            thumbPane.repaint();
            thumbPane.setSelection(-1,-1);
        }
    }

    /**
     * Creates new form ImageThumbPane
     * @param rows: Anzahl Reihen
     * @param cols: Anzahl Spalten
     */
    public ImageThumbPane(int rows, int cols) {
        this.selection = new int[]{-1, -1};
        this.rows = rows;
        this.cols = cols;
        
        initComponents();
        final Object _this = this;
        DropTarget dt = new DropTarget(this, new DropTargetListener(){
            
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                try{
                    File f = new File(dtde.getTransferable().getTransferData(ImageStringSelection.flavor).toString());
                    dtde.acceptDrag(dtde.getDropAction());
                } catch (Exception ex){
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
               
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                // Falls Quelle == Ziel: Nichts machen
                DropTarget dt = (DropTarget)dtde.getSource();
                if (dt.getComponent() == _this){
                    return;
                }
                
                 try {
                    Transferable tr = dtde.getTransferable();
                    DataFlavor[] flavors = tr.getTransferDataFlavors();
                    for (int i = 0; i < flavors.length; i++)
                     if (flavors[i].isMimeTypeEqual(ImageStringSelection.flavor)) {
                      // Zunächst annehmen
                      dtde.acceptDrop (dtde.getDropAction());
                      String file = ((URI)tr.getTransferData(flavors[i])).toString();
                      
                      String fname = FileManager.makeImageRepoPathAbsolute(file);
                      Path imgPath = Paths.get(fname);
                      if (Files.exists(imgPath)){
                            ImageThumbPane comp = (ImageThumbPane)((DropTarget)dtde.getSource()).getComponent();
                            if (Files.exists(Paths.get(currentDir+"/"+imgPath.getFileName()))){
                                int response = JOptionPane.showConfirmDialog(comp,
                                "The file " + imgPath.getFileName() + 
                                " already exists. Do you want to replace the existing file?",
                                "Ovewrite file", JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                                if (response != JOptionPane.YES_OPTION){
                                    return;
                                }
                            }
                            FileManager.xcopy(imgPath.toString(), currentDir+"/"+imgPath.getFileName(), null);
                            clear();
                            scan(currentDir);
                      }
                      
                      dtde.dropComplete(true);
                      return;
                     }
                  } catch (Throwable t) { t.printStackTrace(); }
                  // Ein Problem ist aufgetreten
                  dtde.rejectDrop();
            }
            
        });
        setDropTarget(dt);
    }
    
    public void setSelection(int from, int to){
        this.selection = new int[]{from, to};
        int minimum = (from>to) ? to : from;
        int maximum = (from>to) ? from : to;
        for (Component comp : getComponents()){
            ((ImageThumb)comp).setSelected(false);
        }
        for (int i=Math.max(minimum,0);i<Math.min(maximum+1,getComponentCount());i++){
            ((ImageThumb)getComponent(i)).setSelected(true);
        }
    }
     protected void onMousePressed(MouseEvent me) {
         int idx = getComponentIndex(me.getComponent());
        if (me.isPopupTrigger()){
            return;
        }
        
        if (me.isControlDown()){
            
        }else if (me.isShiftDown()){
            
        }else{
            setSelection(idx, idx);
        }
    }
    public int getComponentIndex(Component component) {
        if (component != null && component.getParent() != null) {
          for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == component)
              return i;
          }
        }
        return -1;
  }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0);
        flowLayout1.setAlignOnBaseline(true);
        setLayout(flowLayout1);
    }// </editor-fold>//GEN-END:initComponents
    public void AddImage(File image){
        ImageThumb img = new ImageThumb(image.getPath());
        img.addMouseListener(new ThumbMouseListener(this));
        this.add(img);
    }
    public void clear(){
        this.removeAll();
    }
    public void SetFileTree(FileTree tree){
        fileTree = tree;
        tree.addTreeSelectionListener(new FileTreeSelectionListener(this));
    }
    public void scan(Path directory){
        if (!Files.isDirectory(directory)){
            return;
        }
        DirectoryStream<Path> stream;
        try{
            stream = Files.newDirectoryStream(directory, new DirectoryStream.Filter<Path>() {
            private final String[] FILE_EXTENSIONS = {".png", ".jpg", ".bmp", ".gif"};
            PathMatcher matcher = fileTree.getFilePathMatcher();
            
            @Override
            public boolean accept(Path entry)
            {
                if (Files.isDirectory(entry) || !matcher.matches(entry.getFileName())){
                    return false;
                }
                String tmp = entry.toString().toLowerCase();
                for (String ext:FILE_EXTENSIONS){
                    if (tmp.endsWith(ext)) return true;
                }
                return false;
            }
            });
        }catch (IOException e){
            return;
        }
        for(Path file: stream){
            AddImage(file.toFile());
        }
        doLayout();
        repaint();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}
