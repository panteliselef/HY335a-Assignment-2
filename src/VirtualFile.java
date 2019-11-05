/*
 * Pantelis Eleftheriadis
 * csd3942
 */
import java.util.Date;

public class VirtualFile {
    private String name;
    private String content;
    private long timestamp;
    VirtualFile(String name,String content){
        this.name = name;
        this.content = content;
        this.timestamp = new Date().getTime();
    }
    VirtualFile(String name,String content,long timestamp){
        this.name = name;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "VirtualFile: "+name;
    }
}
