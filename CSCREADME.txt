Language:
	seeming to be Java, as it has easy and cross platform socket programming capabilities

Plan in more detail:
	-have a server program that stores history in a file and I/O's to it when something happens.
		-server will aim to have each client on a separate thread in the server to allow
		 real-time updates in the chat room
		-when an update happens in the chat room, server sends the new message through each client thread
	-have a client server that connects to the server and sends/recieves messages to the server
	

	-for connectivity and threading, use the Java libraries Socket and Thread/Runnable. 
		-concurrency will require some form of concurrency planning.


NOTES:
	The client handler in the server is what is actually recieving the messages the client recieves. 
	The problem is, the hander cannot see other handlers, and when these handlers are made, there are
	no usable references made to them. In order for the server to send messages to all of the handler 
	threads, the handlers themselves must be stored in a way that is accessible to the server, i.e.
	an array list.

	Clients are now stored in an array list, and clients know their position. Whenever the server
	gets a message, it needs to systematically send the message to all positions in the array list
	except the position that sent the message. We can acheive this by asking the message sender for their
	ID. (Maybe the handlers can send messages to each other by having a reference to the list)

	Since ID's are based on array size, dupe IDs can be made when a client leaves. To remedy this, we
	can either make a new system, or more easily, find a way to recycle IDs by placing a client placeholder 
	where a client leaves, and when a new client joins, if there are any, the new client takes the spot
	of the closest placeholder to 0, or oldest placeholder.

	

	NOTE: 
		professor said he doesnt care about real-time he just wants
		communication. He wants to have channel-based communication like discord where instead of
		real time communication clients should join with username and password and have access to
		read and write to channel chat history. We will now need significantly less threading than
		we thought since we dont need real time communication.

		"channel" can be a syncro-protected text file for chat history. Whenever
		the server gets a message, the client handler writes the message in an
		orderly fashion to the history log
