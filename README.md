# Proxy Server
### Abstract
Implement a server-client communication where the client sends a URL, and the server reads, caches, and sends the image back as byte packets using TCP and TFIP regulations.   

### Features
- TCP Sliding Window
- TCP Retransmission Timeout
- Simple XOR Encryption with XOR Shift
- Able to emulate packet loss 
- Out of order packet handling
- Server Caching

## Results
[HTML-Preview](https://htmlpreview.github.io/?https://github.com/s-Aura-v/ProxyServer/blob/main/src/main/resources/data/index.html)

### Run Guide
To compile the program, use the command
`$ mvn clean install`

Run the server using
`$ java -jar target/Server.jar`

Run the client using 
`$ java -jar target/Client.jar`

You can edit the PORT and SERVER in Client.java. 

####Reactor-Pattern
Reactor Pattern has been deprecated. While it works with single images, it fails when you send multiple images because it did not properly process all of the acks. 

### Challenges
1. Understanding reactor-pattern with SelectionKeys and Selectors
2. Properly clear the buffer before starting a new session
3. Missing important properties
	It might be better to work on everything from the start. I tend to work on programs bit by bit, moving around and creating methods. But I should work on the full functionality from the beginning to stop broken code.

### Resources
https://gee.cs.oswego.edu/dl/csc445/a2.html
https://datatracker.ietf.org/doc/html/rfc6298 
https://docs.google.com/document/d/1w2aBgG3_AVqI-vrXVIz434PII86Ekfp_DPa6dKXhxRQ/edit?tab=t.0
