/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide.imagerepo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mario
 */
public class TagDataBase {
    Path dbPath;
    Connection conn = null;
    
    class ImageFileWalker extends SimpleFileVisitor<Path>{
        String ATTR_NAME_TAGS = "Sikuli_ImageTags";
        private Connection c;
        private TagDataBase db;
        public ImageFileWalker(TagDataBase db, Connection c){
            this.c = c;
            this.db = db;
        }
        @Override
        public FileVisitResult visitFile(Path t, BasicFileAttributes bfa) throws IOException {
            String[] tags;
            try {
                UserDefinedFileAttributeView attrs = Files.getFileAttributeView( t.toAbsolutePath(),
                                       UserDefinedFileAttributeView.class );
                ByteBuffer attrValue  = ByteBuffer.allocate(attrs.size(ATTR_NAME_TAGS));
                attrs.read(ATTR_NAME_TAGS, attrValue);
                attrValue.rewind();
                String value = StandardCharsets.UTF_8.decode( attrValue ).toString();
                tags = value.split(",");
            } catch (IOException ex) {
                // no tags meta data for the file -> just continue
                tags = new String[]{};
            }
            try{
                Statement stmt = conn.createStatement();
                String sql = "INSERT INTO images (dir, name) VALUES ( '%s', '%s')"; 
                stmt.executeUpdate(String.format(sql, t.getParent(), t.getFileName()));
                ResultSet res = stmt.getGeneratedKeys();
                stmt.close();
                int imgId = res.getInt(1);
                sql = "INSERT INTO tags VALUES (?,?)";
                PreparedStatement tagStmt = conn.prepareStatement(sql);
                
                for (String tag:tags){
                    tagStmt.setInt(1, imgId);
                    tagStmt.setString(2, tag);
                    tagStmt.execute();
                }
                tagStmt.close();
            } catch (SQLException ex) {
                Logger.getLogger(TagDataBase.class.getName()).log(Level.SEVERE, null, ex);
            }
            return FileVisitResult.CONTINUE;
        }
        
    }
    
    public TagDataBase(){
    }
    protected Connection getConnection(Path path) throws ClassNotFoundException, SQLException{
        if (conn == null || conn.isClosed()){
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath().toString());
        }
        return conn;
    }
    public void setPath(Path path){
        if(conn != null){
            try{
                conn.commit();
                conn.close();
            }catch(Exception e){}
        }
        dbPath = path;
    }
    private void createNew(Path path) throws IOException, ClassNotFoundException, SQLException{
        if (Files.exists(path)){
            if (conn!=null && !conn.isClosed()) conn.close();
            Files.delete(path);
        }
        conn = getConnection(path);
        Statement stmt = conn.createStatement();
        String sql = "CREATE TABLE images " +
                     "(id INTEGER PRIMARY KEY AUTOINCREMENT      NOT NULL," +
                     " dir           TEXT    NOT NULL, " + 
                     " name            TEXT     NOT NULL)";
        stmt.executeUpdate(sql);
        sql =   "CREATE TABLE tags " +
                "(image_id NOT NULL references images(id)," +
                " name TEXT NOT NULL)";
        stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }
    public void createIndex(Path imageBase){
        try{
            createNew(dbPath);
            Connection c = getConnection(dbPath);
            ImageFileWalker imageWalker = new ImageFileWalker(this, c);
            Files.walkFileTree(imageBase, imageWalker);
            conn.close();
            System.out.println("Index erstellt");
        }catch(IOException e){
            Logger.getLogger(TagDataBase.class.getName()).log(Level.SEVERE, 
                    "Auf die Indexdatei kann nicht zugegriffen werden", e);
        }catch(Exception e){
            Logger.getLogger(TagDataBase.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public ArrayList<String> search(String filenamePattern, String[] tags){
        try {
            // Create tag list with 
            String tagsList = "";
            Iterator<String> iter = Arrays.asList(tags).iterator();
            tagsList += iter.hasNext() ? String.format("('%s'", iter.next()) : "";
            while (iter.hasNext()) {
                tagsList += String.format(",'%s'", iter.next());
            }
            Connection c = getConnection(dbPath);
            String query = String.format("SELECT DISTINCT dir||'\\\\'||images.name" +// 
                                         " FROM images " +
                                         " LEFT OUTER JOIN tags ON images.id=tags.image_id" +
                                         " WHERE images.name GLOB '%s'",
                                         filenamePattern);
            if (tags.length>0) query += " AND tags.name IN " + tagsList + ")";
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            ArrayList<String> result = new ArrayList<String>();
            while ( rs.next() ) {
                result.add(rs.getString(1));
            }
            rs.close();
            stmt.close(); 
            return result;
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TagDataBase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(TagDataBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static void main(String args[]) {
        Path dbPath = Paths.get("C:\\tmp\\sikuli\\images\\index.db");
        TagDataBase db = new TagDataBase();
        db.setPath(dbPath);
        db.createIndex(Paths.get("C:\\tmp\\sikuli\\images"));
        db.search("Unbenannt.*", new String[]{"ide", "tags"});
    }
}
