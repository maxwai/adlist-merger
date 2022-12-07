import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
	
	private static final String GIT_FOLDER = "git_folder";
	
	private static final Set<Character> BEGINNING_ILLEGAL_CHARACTERS =
			Set.of('#', '!', '$', '&', '>', '<', ']', '[', '|', '@', '/', '.', ':', '-', ',', '?',
					'_', '%');
	
	private static final List<Function<String, String>> REPLACE_FUNCTIONS = List.of(
			s -> s.replace("\t", " "),
			s -> s.replace("0.0.0.0 ", ""),
			s -> s.replace("127.0.0.1 ", ""),
			s -> s.replace("255.255.255.255 ", ""),
			s -> s.replace("::1 ", ""),
			s -> s.replace("fe80::1%lo0 ", ""),
			s -> s.replace("ff00::0 ", ""),
			s -> s.replace("ff02::1 ", ""),
			s -> s.replace("ff02::2 ", ""),
			s -> s.replace("ff02::3 ", ""),
			s -> s.replace(":: ", ""),
			s -> s.contains("#") ? s.substring(0, s.indexOf("#")) : s
	);
	
	private static final List<Function<String, Boolean>> ILLEGAL_START_PHRASES = List.of(
			s -> s.startsWith("coded by"),
			s -> s.startsWith("Malvertising list by Disconnect"),
			s -> s.startsWith("Blocklist of hostnames")
	);
	
	public static void main(String[] args) {
		try {
			// listOfDomains -> listsNames -> listsNames
			Map<HashSet<String>, List<String>> adLists = XMLParser.getAdList()
					.entrySet()
					.stream()
					.parallel()
					.map(entry -> Map.entry(getAdList(entry.getKey()), entry.getValue()))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
			
			// listsName -> lists of listOfDomains
			Map<String, List<HashSet<String>>> sortedAdLists = new HashMap<>();
			adLists.forEach((domains, list) -> list
					.forEach(listName -> {
						if (!sortedAdLists.containsKey(listName)) {
							sortedAdLists.put(listName, new ArrayList<>());
						}
						sortedAdLists.get(listName).add(domains);
					}));
			
			// listsName -> filtered list of urls
			Map<String, Set<String>> filteredAdLists = new HashMap<>();
			sortedAdLists.forEach((listName, list) -> {
				Set<String> set = list.stream().flatMap(Set::stream).collect(Collectors.toSet());
				filteredAdLists.put(listName, set);
			});
			// write to files
			filteredAdLists.forEach(Main::createFile);
			if (checkGitChanges())
				makeGitCommands();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private static void createFile(String listName, Set<String> urls) {
		final Path currentPath = Path.of(GIT_FOLDER, listName + ".txt");
		try {
			Files.writeString(currentPath, String.join("\n", urls));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static HashSet<String> getAdList(String url) {
		final URLConnection connection;
		try {
			connection = new URL(url).openConnection();
			
			try (InputStream input = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
				return reader.lines()
						.map(String::trim)
						.filter(line -> !line.equals(""))
						.filter(line -> !BEGINNING_ILLEGAL_CHARACTERS.contains(line.charAt(0)))
						.filter(line -> ILLEGAL_START_PHRASES.stream()
								.noneMatch(function -> function.apply(line)))
						.map(line -> REPLACE_FUNCTIONS.stream()
								.reduce(Function.identity(), Function::andThen)
								.apply(line))
						.map(String::trim)
						.filter(line -> !line.equals(""))
						// catch following: medicalxpress.com,techxplore.com
						.flatMap(line -> Arrays.stream(line.split(",")))
						// catch following:  ublock.org www.ublock.org demo.ublock.org
						.flatMap(line -> Arrays.stream(line.split(" ")))
						.peek(line -> {
							if (!isValid(line))
								System.out.println(url + " : " + line);
						})
						.filter(Main::isValid)
						.collect(Collectors.toCollection(HashSet::new));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static boolean checkGitChanges() {
		final String cmd = "git status -s";
		final Runtime runtime = Runtime.getRuntime();
		
		try {
			final Process process = runtime.exec(new String[]{"sh", "-c", cmd}, null,
					new File(GIT_FOLDER));
			
			final String output = doProcess(process);
			
			process.waitFor();
			if (output.equals("\nErrors:\n\n")) {
				System.out.println("No new changes");
				return false;
			} else if (output.contains("\nErrors:\n\n"))
				return true;
			System.err.println(output);
			throw new RuntimeException();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void makeGitCommands() {
		System.out.println("Doing git commands");
		
		final String cmd1 = "git add .";
		final String cmd2 = "git commit -m 'Changes from " + LocalDateTime.now() + "'";
		final String cmd3 = "git push";
		final List<String> cmds = List.of(cmd1, cmd2, cmd3);
		
		final Runtime runtime = Runtime.getRuntime();
		
		cmds.forEach(cmd -> {
			try {
				final Process process = runtime.exec(new String[]{"sh", "-c", cmd}, null,
						new File(GIT_FOLDER));
				
				final String output = doProcess(process);
				
				System.out.println("Exit value: " + process.waitFor());
				System.out.println(output);
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		System.out.println("Finished with Git commands");
	}
	
	private static String doProcess(Process process) throws IOException {
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()));
		
		final BufferedReader errorreader = new BufferedReader(
				new InputStreamReader(process.getErrorStream()));
		
		final StringBuilder output = new StringBuilder();
		
		String line;
		while ((line = reader.readLine()) != null) {
			output.append(line).append("\n");
		}
		
		output.append("\nErrors:\n\n");
		
		while ((line = errorreader.readLine()) != null) {
			output.append(line).append("\n");
		}
		return output.toString();
	}
	
	public static boolean isValid(String url) {
		try {
			new URL("https://" + url).toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
