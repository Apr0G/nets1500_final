Penn Course Planner

Penn Course Planner is a Java-based course planning and course search tool for freshmen Penn students. The user enters their major, minor(s), interests, constraints, maximum CUs per semester, and any courses they want to include or avoid. The program then uses real Penn course and requirement data to generate a semester-by-semester academic plan that tries to satisfy prerequisites, course availability, major/minor requirements, and elective preferences. The project changed from our original proposal because scraping every Penn major and minor perfectly was much harder than expected, so we focused the major-planning component on Engineering majors and made minor data available through a manually corrected JSON file. The final project still follows our original goal: combining web data, graph-based scheduling, and information retrieval to make academic planning easier.

Course Concepts Used

This project uses three main NETS 1500 course concepts: Graph and Graph Algorithms, Information Networks / the World Wide Web, and Document Search / Information Retrieval. We model courses as nodes in a prerequisite graph, where edges represent prerequisite dependencies. The planner uses graph logic, including prerequisite expansion, topological ordering, and semester-level calculations and greedy sorting algorithms, to determine when courses can be taken and to build a valid schedule. We also use the World Wide Web as an information network by scraping public Penn course catalog and requirement pages using JSoup. Finally, we use document search/information retrieval through TF-IDF matching, which ranks elective courses based on the student’s stated interests and course descriptions.

Data Collection and Scraping

A major part of the project was collecting and normalizing Penn course, major, and minor data. Course scraping was the most straightforward part: we scraped Penn’s public course catalog and extracted course IDs, names, credit units, prerequisites, corequisites, mutually exclusive courses, cross-listings, descriptions, and available semester information when it existed. However, different courses often had missing or inconsistent information, so the planner includes warning messages when semester availability, yearly offering status, or other details are uncertain. Because the amount of scraping is large, the app may occasionally stop partway through; if this happens, please stop and rerun the app.

Scraping major requirements was much harder. Every Engineering major had its own webpage format, requirement structure, elective categories, exceptions, concentration rules, and “choose one/choose many” logic. Because of this, the major scraper required a lot of custom logic and some hardcoding for specific majors. We used AI assistance for the major scraping logic only to help reason through difficult scraping cases and explain how to structure complicated requirement rules, such as grouped electives, concentrations, and combined Math/Natural Science/Engineering elective lists. The basic scraping structure and implementation were written by us, but AI helped us debug and design some of the more difficult parsing logic. For this reason, we limited the automated major requirement scraper to Engineering majors, and even then some tiny bugs or inconsistencies may remain because the public webpages were not standardized. We also only used public sources and did not have access to Path@Penn or internal Penn planning data.

For minors, our goal was to make all minors available for selection. Since there were too many minors and their pages were also inconsistent, we did not write a full scraper for every minor. Instead, we used AI assistance to extract the minor requirement schemas, then manually reviewed and corrected the resulting JSON as much as possible. The final planner uses these uploaded JSON schemas rather than dynamically scraping every minor page during runtime.

How It Works

The planner reads JSON files for courses, majors, and minors. Major and minor requirements are stored as flat rule entries, such as REQUIRED_ALL, CHOOSE_ONE, CHOOSE_N, and grouped options. The planner resolves prerequisites, avoids double-counting within a program, checks course equivalences, respects semester availability when known, and greedily fills semesters while staying under the user’s maximum CU limit. For electives, it uses TF-IDF to compare the user’s interests with course text and prioritize more relevant classes. The output is a formatted semester-by-semester schedule, along with warnings when data is incomplete or a course may be difficult to schedule.
Specifically, planner works in the following manner:

1. Major & Minor Requirements - Parses flat JSON rule entries, (REQUIRED_ALL, CHOOSE_ONE, CHOOSE_N) to identify what courses must be taken and which can be chosen
2. Prerequisite Graph - Models courses as a DAG with prerequisite dependencies, computing the earliest semester each course can be taken using topological sorting and DFS-based level computation
3. Smart Elective Selection - Uses TF-IDF (Information Retrieval) to rank electives by relevance to your stated interests, normalized within choice groups to prevent domination by individual terms
4. Constraint Satisfaction - Respects max CUs per semester, course availability windows (Fall/Spring), course equivalences, and excluded courses
5. Auto-Scheduling - Greedily fills semesters in topological order, auto-extending beyond the initial plan if needed (up to 16 semesters) to fit everything.

The whole pipeline runs in-memory, with no external dependencies beyond the JSON data files.


Tech Stack

Java 17 with Maven
Gson 2.10.1 for JSON parsing
JavaFX 21 for the conversational UI
JSoup for web scraping
TF-IDF for interest-based course search and elective ranking

Running It

Run the following command in the terminal:

mvn javafx:run

Then answer the chatbot’s questions about your major, minors, completed courses, desired courses, excluded courses, interests, maximum CUs per semester, and starting semester. The planner will generate a course plan and display it in a formatted table.

Work Breakdown

Arina Velieva: Responsible for course, major, and minor data collection and preprocessing. She scraped Penn course data, worked on Engineering major requirement scraping, normalized requirement rules into JSON format, manually reviewed minor schemas, and handled many data cleaning/debugging issues.

Faig Safiyev: Responsible for the graph-based planning and scheduling engine. He built the prerequisite graph logic, scheduling algorithm, TF-IDF elective matching, constraint handling, and semester-by-semester course planning logic.

Both: Tested and debugged the full pipeline, refined the chatbot interface, checked edge cases, adjusted requirement handling, and worked on the final write-up and project presentation.

AI Usage

We used AI assistance in specific parts of the project. AI was used most heavily for UI support - we created an interactive chatpot for easy and convenient user input collection. We aslo used it for helping reason through difficult scraping logic, especially when major requirement pages had inconsistent formats or complicated grouped elective rules. We also used AI to help extract initial minor requirement schemas, but we manually reviewed and corrected those JSON files afterward. AI was not used as a replacement for the core planner: the graph scheduling logic, TF-IDF matching, course catalog  and most of the major scraping structure, JSON integration, and debugging were implemented and tested by us. 
