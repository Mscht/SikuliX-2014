/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide.imagerepo;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.MNEMONIC_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.TOP;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.sikuli.basics.FileManager;
import org.sikuli.ide.ImageStringSelection;

/**
 *
 * @author Mario
 */
public class FileTree extends JTree {
    private String rootPath; 
    private PathMatcher matcher;
    
    public static class DirectoryTreeNode extends DefaultMutableTreeNode{
        private String hint;
        private final ArrayList<String> filterNames = new ArrayList<String>();

        public static DirectoryTreeNode createDir(Path path, boolean allowRenaming){
            File dir = path.toFile();
            if (dir.exists() && !allowRenaming) return null;
            int i = 0;
            while (dir.exists()){
                dir = new File(path.toString() + String.format(" (%d)", i));
                i++;
            }
            DirectoryTreeNode node = new DirectoryTreeNode(Paths.get(dir.getPath()).getFileName());
            node.setUserObject(Paths.get(dir.getPath()));
            return node;
        }

        public DirectoryTreeNode(Path name){
            super(name);
        }
        @Override
        public String toString() {
            return ((Path)getUserObject()).getFileName().toString() + (hint!=null?hint:"");
        }
        public String getName(){
            return ((Path)getUserObject()).getFileName().toString();
        }

        @Override
        public void setUserObject(Object o) {
            if (o instanceof String){
                Path path = ((Path) getUserObject());
                Path oldFilename = path.getFileName();
                if (!oldFilename.toString().equalsIgnoreCase((String) o)){
                    Path newPath = Paths.get(path.toString(), "/..", (String)o).normalize();
                    if (!newPath.toFile().exists()){
                        setUserObject(newPath);
                        path.toFile().renameTo(newPath.toFile());
                    }
                }
            }else{
                super.setUserObject(o);
            }
        }
        
        public void addFilterName(String filterName){
            filterNames.add(filterName);
            setHint(String.format("(%d)", filterNames.size()));
        }
        
        public void setHint(String hint){
            this.hint = "<" + hint + ">";
        }
        public String getHint(){
            return hint;
        }
        
        public void createIfnExists(){
            try{
                Path dir = (Path) getUserObject();
                if (!dir.toFile().exists()) dir.toFile().mkdir();
                //setUserObject(Paths.get(dir.getPath()));
                
            } catch(Exception e){
                
            }
        }
        
   }
    public static class DirectoryTreeModel extends DefaultTreeModel{

        public DirectoryTreeModel(TreeNode root) {
            super(root);
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            boolean valueHasSpaces = ((String) newValue).contains(" ");
            if (!valueHasSpaces) super.valueForPathChanged(path, newValue);
            else System.out.println("Directory name must not have spaces");;
        }
        
    }
    /**
     * Creates new form FileTree
     * @param filePath
     */
    public FileTree(String filePath) {
        initComponents();
        this.setShowsRootHandles(true);
        this.setEditable(true);

        if (filePath!=null && !filePath.equals("")){
            //super(scan(Paths.get(filePath)));
            rootPath = filePath;
            this.Refresh();
        }
        this.setCellRenderer(new DefaultTreeCellRenderer(){

            @Override
            public Icon getLeafIcon() {
                return this.getClosedIcon();
            }

            @Override
            public void paintComponent(Graphics grphcs) {
                String fullText = this.getText();
                String[] splitText = fullText.split("<");
                
                if(splitText.length==1){
                    super.paintComponent(grphcs);
                }
                else{
                    Graphics2D g2d = (Graphics2D)grphcs;
                    FontMetrics metric = this.getFontMetrics(this.getFont());
                    Rectangle2D rect = metric.getStringBounds(splitText[0], g2d);
                    float pos1 = LEFT + getIcon().getIconWidth() + 2;
                    float pos2 = (float)rect.getX() + (float)rect.getWidth();
                    pos2 += pos1;
                    
                    getIcon().paintIcon(null, grphcs, LEFT-2, TOP-1);
                    g2d.drawString(splitText[0], pos1, metric.getHeight()-CENTER/2-2);
                    g2d.setColor(Color.GRAY);
                    g2d.drawString(" " + splitText[1].replace(">", ""), pos2, metric.getHeight()-CENTER/2-2);
                }
            }
            
        });
        DropTarget dt = new DropTarget(this, new DropTargetListener(){
            // Highlighting
            private int highlightedRow = -1;
            private Rectangle dirtyRegion = null;
            private Color highlightColor = new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 100);
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                Point loc = dtde.getLocation();
                boolean highlighted = false;
                Component comp = ((DropTarget)dtde.getSource()).getComponent();
                int row = ((FileTree)comp).getClosestRowForLocation(loc.x, loc.y);
                //((FileTree)comp).setSelectionRow(row);
                Graphics g = getGraphics();

                // row changed

                if (highlightedRow != row) {
                    if (null != dirtyRegion) {
                        paintImmediately(dirtyRegion);
                    }

                    //for (int j = 0; j){
                     //   if (row == j) {

                        Rectangle firstRowRect = getRowBounds(row);
                        dirtyRegion = firstRowRect;
                        g.setColor(highlightColor);

                        g.fillRect((int) dirtyRegion.getX(), (int) dirtyRegion.getY(), (int) dirtyRegion.getWidth(), (int) dirtyRegion.getHeight());
                        highlightedRow = row;
                      //  } 
                    //}
                }
                
                
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
               paintImmediately(dirtyRegion);
               highlightedRow = -1;
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                 try {
                    Transferable tr = dtde.getTransferable();
                    DataFlavor[] flavors = tr.getTransferDataFlavors();
                    for (int i = 0; i < flavors.length; i++)
                     if (flavors[i].isMimeTypeEqual(ImageStringSelection.flavor)) {
                      // Zunächst annehmen
                      dtde.acceptDrop (dtde.getDropAction());
                      String file = ((URI)tr.getTransferData(flavors[i])).toString();
                      // Wir setzen in das Label den Namen der ersten 
                      // Datei
                      
                      String fname = FileManager.makeImageRepoPathAbsolute(file);
                      Path imgPath = Paths.get(fname);
                      if (Files.exists(imgPath)){
                          Point loc = dtde.getLocation();
                            FileTree comp = (FileTree)((DropTarget)dtde.getSource()).getComponent();
                            TreePath treePath = comp.getClosestPathForLocation(loc.x, loc.y);
                            FileTree.DirectoryTreeNode node = (FileTree.DirectoryTreeNode)treePath.getLastPathComponent();
                            Path dir = (Path)node.getUserObject();
                            if (Files.exists(Paths.get(dir+"/"+imgPath.getFileName()))){
                                int response = JOptionPane.showConfirmDialog(comp,
                                "The file " + imgPath.getFileName() + 
                                " already exists. Do you want to replace the existing file?",
                                "Ovewrite file", JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                                if (response != JOptionPane.YES_OPTION){
                                    return;
                                }
                            }
                            FileManager.xcopy(imgPath.toString(), dir+"/"+imgPath.getFileName(), null);
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
        MouseAdapter ma = new MouseAdapter() {
		private void myPopupEvent(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			final FileTree tree = (FileTree)e.getSource();
			TreePath path = tree.getPathForLocation(x, y);
			if (path == null)
				return;	

			tree.setSelectionPath(path);

			final FileTree.DirectoryTreeNode obj = (FileTree.DirectoryTreeNode)path.getLastPathComponent();

			String label = "Neuer Ordner" ;
                        Action newAction = new AbstractAction()
			{
                            // this is an initializer block
                            {
				putValue(NAME, "New folder");
				putValue(MNEMONIC_KEY, new Integer(KeyStroke.getKeyStroke("N").getKeyCode()));
				putValue(SHORT_DESCRIPTION, "Create a new folder");
				putValue(SMALL_ICON, new ImageIcon("newfile.gif"));
                            }
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {   
                                DirectoryTreeNode newNode = DirectoryTreeNode.createDir(Paths.get(obj.getUserObject().toString()+"\\"+"NewFolder"), true);
                                newNode.createIfnExists();
                                //newNode = new FileTree.DirectoryTreeNode(Paths.get("Neuer Ordner"));
                                //newNode.setUserObject(Paths.get(obj.getUserObject().toString()+"\\"+"Neuer Ordner"));
                                obj.add(newNode);
                                DefaultTreeModel mdl = (DefaultTreeModel) tree.treeModel;
                                mdl.nodesWereInserted(obj, new int[]{obj.getIndex(newNode)});
                                //newNode.createDir(true);
                                tree.expandPath(tree.getSelectionPath());
                                TreePath newSelection = tree.getSelectionPath().pathByAddingChild(newNode);
                                tree.setSelectionPath(newSelection);
                            }
			};
                        Action renameAction = new AbstractAction()
			{
                            // this is an initializer block
                            {
				putValue(NAME, "Rename");
				putValue(MNEMONIC_KEY, new Integer(KeyStroke.getKeyStroke("R").getKeyCode()));
				putValue(SHORT_DESCRIPTION, "Rename");
				putValue(SMALL_ICON, new ImageIcon("renamefile.gif"));
                            }
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {   
                                tree.startEditingAtPath(tree.getSelectionPath());
                            }
			};
                        Action deleteAction = new AbstractAction()
			{
                            // this is an initializer block
                            {
				putValue(NAME, "Delete");
				putValue(MNEMONIC_KEY, new Integer(KeyStroke.getKeyStroke("D").getKeyCode()));
				putValue(SHORT_DESCRIPTION, "Delete");
				putValue(SMALL_ICON, new ImageIcon("delete.gif"));
                            }
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {   
                                try {
                                    TreeNode parentNode = obj.getParent();
                                    int index = parentNode.getIndex(obj);
                                    ((DirectoryTreeNode)obj.getParent()).remove(obj);
                                    DefaultTreeModel mdl = (DefaultTreeModel) tree.treeModel;
                                    mdl.reload(parentNode);
                                    //mdl.nodesWereRemoved(obj, new int[]{index}, new Object[]{obj});
                                    Files.deleteIfExists(Paths.get(obj.getUserObject().toString()));
                                    //DirectoryTreeNode newNode = DirectoryTreeNode.createDir(Paths.get(obj.getUserObject().toString()+"\\"+"Neuer Ordner"), true);
                                } catch (IOException ex) {
                                    Logger.getLogger(FileTree.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
			};
			JPopupMenu popup = new JPopupMenu();
			popup.add(new JMenuItem(newAction));
			popup.add(new JMenuItem(renameAction));
			popup.add(new JMenuItem(deleteAction));
			popup.show(tree, x, y);
		}
		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) myPopupEvent(e);
		}
		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) myPopupEvent(e);
		}
	};
        addMouseListener(ma);
        addKeyListener(new KeyListener(){

           @Override
           public void keyPressed(KeyEvent ke) {}

           @Override
           public void keyReleased(KeyEvent ke) {
               if (ke.getKeyCode() == KeyEvent.VK_F5){
                   FileTree tree = (FileTree)ke.getComponent();
                   tree.Refresh();
                }
           }

           @Override
           public void keyTyped(KeyEvent ke) {}
        });
    }
    public String getRootPath(){
       return rootPath;
   }
   public void setFilePathMatcher(PathMatcher pathMatcher){
       matcher = pathMatcher;
   }
   public PathMatcher getFilePathMatcher(){
       return matcher;
   }
   
   private static MutableTreeNode scan(Path node)
   {
        //DefaultMutableTreeNode 
        FileTree.DirectoryTreeNode ret = new FileTree.DirectoryTreeNode(node.getFileName());
        ret.setUserObject(node);
        DirectoryStream<Path> stream;
        try{
            stream = Files.newDirectoryStream(node, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry)
            {
                return Files.isDirectory(entry);
            }
            });
        }catch (IOException e){
            return ret;
        }
      
        if (Files.isDirectory(node))
            for (Path child: stream)
                if (Files.isDirectory(child)){
                    ret.add(scan(child));
                }
        return ret;
   }
   public void Refresh(){
       super.setModel(new DirectoryTreeModel(scan(Paths.get(rootPath))));
   }
   
   /**
    * Expands all paths in the tree.
    *
    * @see JTree#expandPath(TreePath)
    */
   public void expandAll() {
     cancelEditing();
     final TreeModel tm = getModel();
     final Object root = tm.getRoot();

     /* nothing to expand, if no root */
     if (root != null) {
       expandAllPaths(new TreePath(root), tm);
     }
   }

   /**
    * Opens all paths in the given node and all nodes below that.
    *
    * @param path the tree path to the node to expand
    * @see JTree#expandPath(TreePath)
    */
   public void expandAllPaths(TreePath path) {
     cancelEditing();
     expandAllPaths(path, getModel());
   }
   /**
    * Opens all paths in the given node and all nodes below that.
    *
    * @param path the tree path to the node to expand
    * @param treeModel the tree model
    * @see JTree#expandPath(TreePath)
    */
   protected void expandAllPaths(TreePath path, TreeModel treeModel) {
     expandPath(path);
     final Object node = path.getLastPathComponent();
     final int n = treeModel.getChildCount(node);
     for (int index = 0; index < n; index++) {
       final Object child = treeModel.getChild(node, index);
       if (treeModel.getChildCount(child) > 0) {
         expandAllPaths(path.pathByAddingChild(child));
       }
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

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("<no path set>");
        setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
