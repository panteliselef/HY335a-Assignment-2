import java.time.Instant;

public class VirtualFile {
    private String name;
    private Instant timestamp;
    private String content;
    VirtualFile(String name,String content){
        this.name = name;
        this.content = content;
        this.timestamp = Instant.now();
    }
    VirtualFile(String name,String content,Instant timestamp){
        this.name = name;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public Instant getTimestamp() {
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
