import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

// ══════════════════════════════════════════════════════════════
//  CLASS: DocumentParser
//  Responsibility: Read a document file and return its tokens.
//   Also stores full document content for display feature.
// ══════════════════════════════════════════════════════════════
class DocumentParser {

    public ArrayList<String> tokenize(String filePath) {

        ArrayList<String> tokens = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = br.readLine()) != null) {

                // Skip SGML tags
                if (line.trim().startsWith("<")) continue;

                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // First, extract hyphenated words like "boundary-layer" as whole tokens
                // Then also split into individual parts
                String[] parts = line.split("\\s+"); // split by whitespace first
                for (String part : parts) {

                    // If part contains a hyphen, index the whole hyphenated word too
                    // This fixes Query #6: "boundary-layer"
                    if (part.contains("-")) {
                        String hyphenated = part.replaceAll("[^a-zA-Z0-9\\-]", "").toLowerCase();
                        if (!hyphenated.isEmpty() && !hyphenated.equals("-")) {
                            tokens.add(hyphenated); // add "boundary-layer" as one token
                        }
                    }

                    // Also split by non-word characters (indexes individual words too)
                    String[] words = part.split("\\W+");
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            tokens.add(word.toLowerCase());
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("  [Warning] File not found: " + filePath);
        }

        return tokens;
    }

    /**
     * Reads and returns the FULL content of a document as a single string.
     * Used when user wants to see the complete document content.
     * Skips SGML tag lines (lines starting with '<').
     */
    public String readFullContent(String filePath) {

        StringBuilder content = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = br.readLine()) != null) {

                // Skip SGML tags — user doesn't need to see these
                if (line.trim().startsWith("<")) continue;

                // Add readable lines to content
                if (!line.trim().isEmpty()) {
                    content.append(line.trim()).append("\n");
                }
            }

        } catch (IOException e) {
            return "[Content not available — file not found]";
        }

        return content.toString().trim();
    }
}


// ══════════════════════════════════════════════════════════════
//  CLASS: DocumentEntry
//  Responsibility: Stores a document name + word frequency.
//  This is used inside the inverted index posting lists.
// ══════════════════════════════════════════════════════════════
class DocumentEntry {

    String docName;
    int frequency;
    DocumentEntry(String docName) {
        this.docName = docName;
        this.frequency = 1;
    }
}


// ══════════════════════════════════════════════════════════════
//  CLASS: Document
//  Responsibility: Stores document name + file path + content.
//  NEW: Added to support showing full document content on search.
// ══════════════════════════════════════════════════════════════
class Document {

    String docName;
    String filePath;
    String content;

    Document(String docName, String filePath, String content) {
        this.docName = docName;
        this.filePath = filePath;
        this.content = content;
    }
    public String getDocName() {
        return docName;
    }

    public String getContent() {
        return content;
    }
}


// ══════════════════════════════════════════════════════════════
//  CLASS: InvertedIndex
//  Responsibility: Core data structure of the search engine.
//  Structure:
//    Key   → word (String)
//    Value → list of DocumentEntry objects (which docs contain it)
// ══════════════════════════════════════════════════════════════
class InvertedIndex {

    // ── The core HashMap: word → list of (docName, frequency) ──
    // This is O(1) average for both insert and lookup.
    private HashMap<String, ArrayList<DocumentEntry>> index;

    // ── Stop words: common words we do NOT index ─────────────
    // These words appear in almost every document and have no
    // search value. Removing them reduces index size significantly.
    private HashSet<String> stopWords;

    // ── Track total unique words indexed ─────────────────────
    private int totalUniqueWords;

    /**
     * Constructor — initializes the HashMap and stop word list.
     */
    public InvertedIndex() {
        this.index = new HashMap<>();
        this.totalUniqueWords = 0;

        // Initialize stop words set
        this.stopWords = new HashSet<>(Arrays.asList(
                "the", "a", "an", "and", "or", "is", "in", "on",
                "at", "to", "of", "for", "it", "this", "that",
                "was", "are", "with", "as", "by", "be", "has", "had",
                "from", "but", "not", "they", "we", "he", "she", "its"
        ));
    }

    /**
     * Adds a word from a specific document into the inverted index.
     * If word already exists for this doc → increment frequency.
     * If word is new for this doc → add new DocumentEntry.
     * If word is completely new → create new entry in HashMap.
     *
     * Time Complexity: O(1) average — HashMap put/get operations.
     *
     * @param word    - the token to index
     * @param docName - which document this word came from
     */
    public void addWord(String word, String docName) {

        // Skip stop words
        if (stopWords.contains(word)) return;

        // Skip very short words (single characters are noise)
        if (word.length() < 2) return;

        // FIX 2: HashMap-based lookup — O(1) average
        if (index.containsKey(word)) {

            // Word already exists in index — check if this doc is already recorded
            ArrayList<DocumentEntry> postingList = index.get(word);

            for (DocumentEntry entry : postingList) {
                if (entry.docName.equals(docName)) {
                    // This doc already has this word — just increment frequency
                    entry.frequency++;
                    return;
                }
            }

            // Word exists but not in this doc yet — add new entry
            postingList.add(new DocumentEntry(docName));

        } else {

            // Completely new word — create posting list and add to index
            ArrayList<DocumentEntry> newList = new ArrayList<>();
            newList.add(new DocumentEntry(docName));
            index.put(word, newList);
            totalUniqueWords++;
        }
    }

    /**
     * Searches for a word and returns its posting list (sorted by frequency).
     *
     * FIX 3: Returns a COPY of the list — not the live reference.
     * This prevents the sort from mutating the original index data.
     *
     * Time Complexity: O(1) average for HashMap lookup + O(k log k) for sort
     * where k = number of documents containing this word.
     *
     * @param word - the search term
     * @return sorted list of DocumentEntry (highest frequency first)
     */
    public ArrayList<DocumentEntry> search(String word) {

        word = word.toLowerCase().trim();

        if (!index.containsKey(word)) {
            return new ArrayList<>(); // word not found
        }

        // FIX 3: Make a COPY — do NOT sort the original list in the index
        ArrayList<DocumentEntry> result = new ArrayList<>(index.get(word));

        // Sort by frequency (highest first) using bubble sort
        // Bubble sort is O(n²) but acceptable for small posting lists
        for (int i = 0; i < result.size() - 1; i++) {
            for (int j = 0; j < result.size() - i - 1; j++) {
                if (result.get(j).frequency < result.get(j + 1).frequency) {
                    DocumentEntry temp = result.get(j);
                    result.set(j, result.get(j + 1));
                    result.set(j + 1, temp);
                }
            }
        }

        return result;
    }

    /**
     * Prints the full inverted index to console.
     * Useful for debugging and demonstrating the data structure.
     */
    public void printIndex() {
        System.out.println("\n============ INVERTED INDEX ============");
        for (String word : index.keySet()) {
            System.out.print(word + " → ");
            for (DocumentEntry entry : index.get(word)) {
                System.out.print(entry.docName + "(" + entry.frequency + ") ");
            }
            System.out.println();
        }
        System.out.println("Total unique words indexed: " + totalUniqueWords);
        System.out.println("========================================\n");
    }

    /**
     * Returns total number of unique words in the index.
     */
    public int getTotalUniqueWords() {
        return totalUniqueWords;
    }
}


// ══════════════════════════════════════════════════════════════
//  CLASS: QueryProcessor
//  Responsibility: Parse and execute user search queries.
//
//  FIX 4: Multi-keyword AND query now supported.
//  FIX 5: Robust query parsing (handles extra spaces, uppercase).
//  NEW: Returns document content alongside document names.
// ══════════════════════════════════════════════════════════════
class QueryProcessor {

    // Reference to the inverted index (for lookups)
    private InvertedIndex index;

    // Reference to document store (for fetching full content)
    // Key = docName (e.g. "Doc1"), Value = Document object
    private HashMap<String, Document> documentStore;

    /**
     * Constructor.
     *
     * @param index         - the built inverted index
     * @param documentStore - map of docName → Document (for content display)
     */
    public QueryProcessor(InvertedIndex index, HashMap<String, Document> documentStore) {
        this.index = index;
        this.documentStore = documentStore;
    }

    /**
     * Main query entry point.
     * Detects AND, OR, or single keyword and routes accordingly.
     *
     * FIX 4 & 5: Query parsing is now robust — handles:
     *   - Extra spaces between words and operators
     *   - Uppercase AND/OR from user input
     *   - Multi-keyword AND: "word1 AND word2 AND word3"
     *
     * @param rawQuery - user's raw input string
     * @return list of matching document names
     */
    public ArrayList<String> processQuery(String rawQuery) {

        // Normalize: trim whitespace, collapse multiple spaces, lowercase
        String query = rawQuery.trim().replaceAll("\\s+", " ").toLowerCase();

        // FIX 5: detect operators after normalization
        if (query.contains(" and ")) {
            return andQuery(query);  // handles multi-keyword AND
        }

        if (query.contains(" or ")) {
            String[] parts = query.split(" or ", 2);
            String word1 = parts[0].trim();
            String word2 = parts[1].trim();
            return orQuery(word1, word2);
        }

        // Single keyword
        return singleQuery(query.trim());
    }

    /**
     * Single keyword search.
     * Returns document names where this word appears.
     */
    private ArrayList<String> singleQuery(String word) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<DocumentEntry> entries = index.search(word);

        for (DocumentEntry entry : entries) {
            result.add(entry.docName);
        }
        return result;
    }

    /**
     * AND query — Intersection.
     * FIX 4: Now supports multiple AND keywords.
     * "word1 AND word2 AND word3" → docs containing ALL three.
     *
     * Algorithm:
     *   1. Split query by " and "
     *   2. Get posting list for first keyword
     *   3. For each additional keyword, intersect with running result
     *   4. Return final intersection
     *
     * Time Complexity: O(k × n) where k=keywords, n=avg posting list size.
     */
    private ArrayList<String> andQuery(String query) {

        // Split on " and " to support multiple keywords
        String[] keywords = query.split(" and ");

        // Start with posting list of first keyword
        String firstWord = keywords[0].trim();
        ArrayList<DocumentEntry> firstList = index.search(firstWord);

        // Put first list into a HashSet for fast lookup
        HashSet<String> resultSet = new HashSet<>();
        for (DocumentEntry entry : firstList) {
            resultSet.add(entry.docName);
        }

        // For each additional keyword, keep only docs in BOTH sets (intersection)
        for (int i = 1; i < keywords.length; i++) {
            String word = keywords[i].trim();
            ArrayList<DocumentEntry> currentList = index.search(word);

            // Docs in currentList
            HashSet<String> currentSet = new HashSet<>();
            for (DocumentEntry entry : currentList) {
                currentSet.add(entry.docName);
            }

            // Intersection: keep only docs that appear in BOTH
            resultSet.retainAll(currentSet);
        }

        return new ArrayList<>(resultSet);
    }

    /**
     * OR query — Union.
     * Returns documents containing EITHER word.
     *
     * Time Complexity: O(|A| + |B|) — linear.
     */
    private ArrayList<String> orQuery(String word1, String word2) {

        ArrayList<DocumentEntry> list1 = index.search(word1);
        ArrayList<DocumentEntry> list2 = index.search(word2);

        // HashSet automatically removes duplicates
        HashSet<String> resultSet = new HashSet<>();

        for (DocumentEntry entry : list1) resultSet.add(entry.docName);
        for (DocumentEntry entry : list2) resultSet.add(entry.docName);

        return new ArrayList<>(resultSet);
    }

    /**
     * Displays search results in a clean, readable format.
     * NEW FEATURE: Also shows the full content of each matching document.
     *
     * How it works:
     *   1. Shows query and count of matching documents.
     *   2. For each matching document:
     *      a. Shows document name
     *      b. Fetches Document object from documentStore
     *      c. Prints full content of that document
     *
     * @param rawQuery - original query (for display)
     * @param results  - list of matching document names
     */
    public void displayResults(String rawQuery, ArrayList<String> results) {

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("  Query: \"" + rawQuery + "\"");
        System.out.println("╚══════════════════════════════════════════════════╝");

        if (results.isEmpty()) {
            System.out.println("  No documents found for this query.");
            System.out.println("──────────────────────────────────────────────────\n");
            return;
        }

        System.out.println("  Found: " + results.size() + " document(s)\n");

        // ── Show each result with full document content ──────
        for (int i = 0; i < results.size(); i++) {

            String docName = results.get(i);

            System.out.println("  ┌─────────────────────────────────────────────┐");
            System.out.println("  │  Result " + (i + 1) + ": " + docName);
            System.out.println("  └─────────────────────────────────────────────┘");

            // NEW: Fetch and display full document content
            if (documentStore.containsKey(docName)) {
                Document doc = documentStore.get(docName);
                String content = doc.getContent();

                System.out.println("  ── Document Content ──────────────────────────");

                // Print content line by line with indentation for readability
                String[] lines = content.split("\n");
                for (String line : lines) {
                    System.out.println("  " + line);
                }

                System.out.println("  ──────────────────────────────────────────────");
            } else {
                System.out.println("  [Content not available]");
            }

            System.out.println();
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  CLASS: Main
//  Responsibility: Entry point. Builds index, runs search loop.
//
//  FIX 6: Corrected "3 documents" message → shows actual count.
//  NEW: documentStore HashMap stores all documents + their content.
// ══════════════════════════════════════════════════════════════
public class Main {

    public static void main(String[] args) {

        // ── Setup ───────────────────────────────────────────────
        DocumentParser parser = new DocumentParser();
        InvertedIndex index = new InvertedIndex();

        // NEW: Document store — maps docName → Document object (with content)
        // This allows QueryProcessor to fetch and display document content.
        HashMap<String, Document> documentStore = new HashMap<>();

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║          Mini Search Engine — Loading...         ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        // ── Phase 1 + 2: Parse & Index all 50 documents ────────
        int successCount = 0;

        for (int i = 1; i <= 50; i++) {

            String filePath = "documents/Doc" + i + ".txt";
            String docName  = "Doc" + i;

            // Tokenize the document and add to index
            ArrayList<String> tokens = parser.tokenize(filePath);

            if (!tokens.isEmpty()) {
                // Add all tokens to the inverted index
                for (String token : tokens) {
                    index.addWord(token, docName);
                }

                // NEW: Read and store full document content for display
                String fullContent = parser.readFullContent(filePath);
                documentStore.put(docName, new Document(docName, filePath, fullContent));

                successCount++;
                System.out.println("  " + docName + " indexed ✓  (" + tokens.size() + " tokens)");
            }
        }

        // FIX 6: Show actual count — not hardcoded "3"
        System.out.println("\n✅ Indexing complete!");
        System.out.println("   Documents indexed : " + successCount);
        System.out.println("   Unique words      : " + index.getTotalUniqueWords());

        // ── Phase 3: Query Processor ────────────────────────────
        QueryProcessor qp = new QueryProcessor(index, documentStore);

        // ── Live Search Loop ────────────────────────────────────
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║            MINI SEARCH ENGINE — Ready            ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║  Commands:                                       ║");
        System.out.println("║   • Single word  : flow                          ║");
        System.out.println("║   • AND query    : flow AND stream               ║");
        System.out.println("║   • OR query     : velocity OR speed             ║");
        System.out.println("║   • Multi-AND    : the AND boundary AND layer    ║");
        System.out.println("║   • Show index   : showindex                     ║");
        System.out.println("║   • Exit         : exit                          ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        while (true) {

            System.out.print("Search > ");
            String rawQuery = scanner.nextLine().trim();

            // Handle special commands
            if (rawQuery.equalsIgnoreCase("exit")) {
                System.out.println("\nThank you for using Mini Search Engine. Goodbye!");
                break;
            }

            if (rawQuery.equalsIgnoreCase("showindex")) {
                index.printIndex();
                continue;
            }

            if (rawQuery.isEmpty()) {
                System.out.println("  Please enter a search term.");
                continue;
            }

            // Process query and display results with full content
            ArrayList<String> results = qp.processQuery(rawQuery);
            qp.displayResults(rawQuery, results);
        }

        scanner.close();
    }
}