

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler {

	public static List<String> allLink = new ArrayList<String>();
	public static String hostName = "cs5700f16.ccs.neu.edu";
	public static String port = "80";
	public static String sesssionId = "";
	public static List<String> secretList = new ArrayList<String>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			if (args.length != 2) {
				throw new Exception("Please ensure that both username and password are entered.");
			}
			String username = args[0];
			String password = args[1];
			System.out.println(username + password);
			Socket neuSocket = null;
			boolean headersSet = false;
			String csrfToken = "";
			int tokenPos = 0;
			
			List<String> keys = null;
			List<String> values = null;
			List<List<String>> keyValues = null;
			List<String> loginRequests = new ArrayList<String>();
			int state = 1;
			loginRequests.add("/accounts/login/?next=/fakebook/");
			loginRequests.add("/accounts/login/");

			for (int i = 0; i < loginRequests.size(); i++) {
				if (state == 1) {

					String linkToParse = loginRequests.get(i);
					neuSocket = createNeuSocket();
					PrintWriter writer = new PrintWriter(neuSocket.getOutputStream());
					BufferedReader br = new BufferedReader(new InputStreamReader(neuSocket.getInputStream()));

					writer.println("GET " + linkToParse + " HTTP/1.1");
					writer.println("Host: cs5700f16.ccs.neu.edu");
					writer.println("Connection: keep-alive");
					writer.println("");
					writer.flush();
					String status = br.readLine();

					if (status.contains("200")) {
						state = 2;
						keyValues = readHeaderOfResponse(br);
						readBodyofResponse(br);
					} else if (status.contains("403") || (status.contains("404"))) {

						throw new Exception("Initial Page Load Error Cannot Proceed Further");
						
					} else if (status.contains("500")) {
						
						loginRequests.add(1, linkToParse);
						
					} else if (status.contains("301") || status.contains("302")) {
						
						keyValues = readHeaderOfResponse(br);
						keys = keyValues.get(0);
						values = keyValues.get(1);
						int pos = 0;
						for (String newStr : keys) {
							++pos;
							if (keys.contains("Location")) {
								linkToParse = values.get(pos);
								break;
							}
						}
						readBodyofResponse(br);
						loginRequests.add(1, linkToParse);
					}

					writer.close();
					br.close();
					neuSocket.close();
				}
				else if(state==2)
				{
					int size=loginRequests.size();
					String linkToParse = loginRequests.get(size-1);
					neuSocket = createNeuSocket();
					PrintWriter writer = new PrintWriter(neuSocket.getOutputStream());
					BufferedReader br = new BufferedReader(new InputStreamReader(neuSocket.getInputStream()));
					
					keys = keyValues.get(0);
					values = keyValues.get(1);

					for (int j = 0; j < keys.size(); j++) {
						if (values.get(j).contains("csrftoken")) {
							tokenPos = j;
							String[] token = values.get(j).split(";");
							csrfToken = token[0].trim();

						}
						
					}
					
					String data = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8") + "&"
							+ URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8") + "&"
							+ URLEncoder.encode("csrfmiddlewaretoken", "UTF-8") + "="
							+ URLEncoder.encode(csrfToken.split("=")[1], "UTF-8") + "&" + URLEncoder.encode("next", "UTF-8")
							+ "=" + URLEncoder.encode("/fakebook/", "UTF-8");

					
					writer.println("POST "+linkToParse+" HTTP/1.1");
					writer.println("Host: cs5700f16.ccs.neu.edu");
					writer.println(keys.get(tokenPos).split("-")[1] + ": " + values.get(tokenPos).split(";")[0] + ";"
							+ (values.get(tokenPos + 1).split(";")[0]).split(":")[0]);
					writer.println("Connection: keep-alive");
					writer.println("Content-Type: application/x-www-form-urlencoded");
					writer.println("Content-Length: 109");
					writer.println("");
					writer.println(data);
					writer.flush();

					
					String status = br.readLine();
					
					if (status.contains("200")) {
						state = 2;
						keyValues = readHeaderOfResponse(br);
						readBodyofResponse(br);
					} else if (status.contains("403") || (status.contains("404"))) {

						throw new Exception("Initial Page Load Error Cannot Proceed Further");
						
					} else if (status.contains("500")) {
						
						state=2;
						loginRequests.add(linkToParse); // adding to back of the list
						
					} else if (status.contains("301")) {
						
						keyValues = readHeaderOfResponse(br);
						keys = keyValues.get(0);
						values = keyValues.get(1);
						int pos = 0;
						for (String newStr : keys) {
							++pos;
							if (keys.contains("Location")) {
								linkToParse = values.get(pos);
								break;
							}
						}
						readBodyofResponse(br);
						loginRequests.add(linkToParse);
						state=2;
						
						
					}
					
								
					else if( status.contains("302")) {
						state=3;
						
						keyValues=readHeaderOfResponse(br);
						readBodyofResponse(br);
								
						
					}
					

					writer.close();
					br.close();
					neuSocket.close();
					
					
					
					
				}
				
				if(state==3)
				{
					String redirectionLink="";
					int size=loginRequests.size();
					
					keys = keyValues.get(0);
					values = keyValues.get(1);
					tokenPos = 0;
					for (int k = 0; k < keys.size(); k++) {

						String searchCookie = keys.get(k);
						if (searchCookie.equalsIgnoreCase("Set-Cookie")) {
							tokenPos = k;
						} else if (searchCookie.equalsIgnoreCase("Location")) {
							redirectionLink = values.get(k);
						}
					}
					
					
					neuSocket = new Socket(hostName, Integer.parseInt(port));
					PrintWriter writer = new PrintWriter(neuSocket.getOutputStream());
					BufferedReader br = new BufferedReader(new InputStreamReader(neuSocket.getInputStream()));

					String sessionIDLoc = keys.get(tokenPos).split("-")[1] + ": " + values.get(tokenPos).split(";")[0];

					System.out.println(redirectionLink);

					writer.println("GET " + redirectionLink + " HTTP/1.1");
					writer.println("Host: cs5700f16.ccs.neu.edu");
					writer.println("Connection: keep-alive");
					writer.println(keys.get(6).split("-")[1] + ": " + values.get(6).split(";")[0]);
					writer.println("");
					writer.flush();

					sesssionId = sessionIDLoc;
					
					readHeaderOfResponse(br);
					String resp = readBodyofResponse(br);
					writer.close();
					br.close();
					neuSocket.close();
					
					Pattern pat = Pattern.compile("[/]fakebook/[0-9]+/");
					Matcher mat = pat.matcher(resp);
					while (mat.find()) {
						allLink.add(resp.substring(mat.start(), mat.end()));
					}

					
					
				}

			}

			crawlLinks();

			for (String str : secretList) {
				Pattern secretFlag = Pattern.compile("FLAG: [0-9a-zA-Z]{64}");
				Matcher scFlag = secretFlag.matcher(str);
				while (scFlag.find()) {
					System.out.println(str.substring(scFlag.start() + 6, scFlag.end()));
				}

			}

		} catch (ConnectException conE) {
			System.out.println("Connection Timed Out Please Retry");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void crawlLinks() {
		// TODO Auto-generated method stub
		int iteration = 0;
		int initialSize = allLink.size();
		int finalSize = allLink.size();
		keepCrawlingLoop();
	}

	private static void keepCrawlingLoop() {
		// TODO Auto-generated method stub
		Socket neuSocket = null;
		List<String> temp = new ArrayList<String>();
		try {

			long startTime = System.currentTimeMillis();

			for (int i = 0; i < allLink.size(); i++) {

				String linkToParse = allLink.get(i);
				neuSocket = createNeuSocket();
				Map<String, String> keyVal = new LinkedHashMap<String, String>();
				System.out.println("iteration=" + i);

				PrintWriter writer = new PrintWriter(neuSocket.getOutputStream());
				// BufferedReader br=new BufferedReader(new
				// InputStreamReader(neuSocket.getInputStream()));

				InputStream inFromServer = neuSocket.getInputStream();
				Scanner scan = new Scanner(inFromServer);

				writer.println("GET " + linkToParse + " HTTP/1.1");
				writer.println("Host: cs5700f16.ccs.neu.edu");
				writer.println("Connection: keep-alive");
				writer.println("Cookie: " + sesssionId);
				writer.println("");
				writer.flush();

				boolean headerStatus = false;
				StringBuilder sb = new StringBuilder();
				while (scan.hasNext()) {
					String resp = scan.nextLine();
					if (resp.length() == 0) {
						break;
					}
					if (headerStatus) {
						keyVal.put(resp.split(": ")[0], resp.split(": ")[1]);
					}

					sb.append(resp + "\n");
					headerStatus = true;
				}

				String header = sb.toString().trim();
				// System.out.println(header);

				if (header.contains("HTTP/1.1 403") || header.contains("HTTP/1.1 404")) {
					continue;
				} else if (header.contains("HTTP/1.1 500")) {
					allLink.add(linkToParse);
				} else if (header.contains("HTTP/1.1 301") || header.contains("HTTP/1.1 302")) {
					allLink.add(keyVal.get("Location"));
				}

				while (scan.hasNext()) {
					String resp = scan.nextLine();

					if (resp.contains("</html>"))
						break;

					Pattern pat = Pattern.compile(
							"([/]fakebook[/][0-9]+[/](friends)?[/]?([0-9]+)?[/]?)|(<h2 class='secret_flag' style=\"color:red\">FLAG: [0-9a-zA-Z]{64}</h2>)");
					Matcher mat = pat.matcher(resp);
					while (mat.find()) {

						String matched = resp.substring(mat.start(), mat.end());

						if (matched.contains("secret")) {
							secretList.add(matched);

						}
						if (secretList.size() == 5) {
							scan.close();
							neuSocket.close();
							writer.close();
							return;
						}

						if (!allLink.contains(matched)) {
							allLink.add(matched);
						}

					}

				}

				writer.close();
				scan.close();
				neuSocket.close();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static Socket createNeuSocket() {
		try {
			Socket neuSocket = null;
			List<List<String>> keyValues;
			InetAddress serverName = InetAddress.getByName(hostName);
			// System.out.println(serverName.getHostAddress());
			neuSocket = new Socket(serverName, Integer.parseInt(port));
			// System.out.println("socket created");
			return neuSocket;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private static void closeSocket(Socket socket) {
		try {
			if (socket != null) {
				socket.close();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String readBodyofResponse(BufferedReader br) {
		// TODO Auto-generated method stub
		try {

			StringBuilder sb = new StringBuilder();

			String line;

			while ((line = br.readLine()) != null) {
				if (line.contains("</html>"))
					break;

				sb.append(line);
			}

			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	private static List readHeaderOfResponse(BufferedReader br) {
		// TODO Auto-generated method stub
		// System.out.println("Reading Header of Response");
		List<List<String>> keyValues = new ArrayList<List<String>>();
		List<String> keys = new ArrayList<String>();
		List<String> values = new ArrayList<String>();
		try {

			String line;

			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				String[] keyVal = line.split(": ");
				if (keyVal.length > 1) {
					keys.add(keyVal[0]);
					values.add(keyVal[1]);
				}

				if ("0".equals(line) || line.isEmpty()) {

					break;

				}

			}
			keyValues.add(keys);
			keyValues.add(values);

			return keyValues;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
