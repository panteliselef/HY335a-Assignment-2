# HY335 Asssignment 2

# ToDo

- [x]  Web Servers should be able to communicate with a "custom" protocol
- [x]  Web Clients should communicate via HTTP protocol with Web Servers

In this assignment I used the classes Socket and ServerSocket that Java provides

# Terminal Commands

### Servers

    java WebServer localhost 3942 "Web Server 1"

    java WebServer localhost 3944 localhost 3942 "Web Server 2"

    java WebServer localhost 3945 localhost 3942 "Web Server 3"

    java WebServer localhost 3946 localhost 3945 "Web Server 4"

### Clients

    java WebClient localhost 3942 < putReq.txt
    java WebClient localhost 3945 < request.txt
    

### End result

Web Server 1 → Web Server 3 → Web Server 4→ Web Server 2

# Test Cases

S1 (has the file)→ S2 (no file | made request) WORKS

    java WebServer localhost 3942 "Web Server 1"
    java WebClient localhost 3942 < putReq.txt
    java WebServer localhost 3944 localhost 3942 "Web Server 2"
    java WebClient localhost 3944 < request.txt
    
    //Everyone should have the file

S1 (has the file)→ S3 (no file)→S2(no file | made request) WORKS

    java WebServer localhost 3942 "Web Server 1"
    java WebClient localhost 3942 < putReq.txt
    java WebServer localhost 3944 localhost 3942 "Web Server 2"
    java WebServer localhost 3945 localhost 3942 "Web Server 3"
    java WebClient localhost 3944 < request.txt
    
    //Everyone should have the file

S1 (has the file)→ S3 (no file | made request)→S2(no file )  WORKS

    java WebServer localhost 3942 "Web Server 1"
    java WebClient localhost 3942 < putReq.txt
    java WebServer localhost 3944 localhost 3942 "Web Server 2"
    java WebServer localhost 3945 localhost 3942 "Web Server 3"
    java WebClient localhost 3945 < request.txt
    
    //Only S1 and S3 should have the file