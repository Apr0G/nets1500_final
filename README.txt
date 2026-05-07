- Penn Course Planner

A course planning tool that builds optimal schedules for Penn students. Instead of manually juggling prerequisites, course availability, and degree requirements, just tell the planner your major, interests, and constraints—and it generates a complete semester-by-semester plan that actually works.

- How It Works

The planner combines graph-based scheduling with TF-IDF matching to build smart course recommendations:

1. Major & Minor Requirements** — Parses flat JSON rule entries, (REQUIRED_ALL, CHOOSE_ONE, CHOOSE_N) to identify what courses must be taken and which can be chosen
2. Prerequisite Graph — Models courses as a DAG with prerequisite dependencies, computing the earliest semester each course can be taken using topological sorting and DFS-based level computation
3. Smart Elective Selection — Uses TF-IDF (Information Retrieval) to rank electives by relevance to your stated interests, normalized within choice groups to prevent domination by individual terms
4. Constraint Satisfaction — Respects max CUs per semester, course availability windows (Fall/Spring), course equivalences, and excluded courses
5. Auto-Scheduling — Greedily fills semesters in topological order, auto-extending beyond the initial plan if needed (up to 16 semesters) to fit everything

The whole pipeline runs in-memory, with no external dependencies beyond the JSON data files.

- Tech Stack

Java 17 with Maven
Gson 2.10.1 for JSON parsing
JavaFX 21 for the conversational UI
JSoup for web-scraping of courses

- Running It

Run the following command in the terminal:
mvn javafx:run

Then just answer the chatbot's questions — major, minors, desired courses, exclusions, interests, max CUs/semester, and start date. The planner will build your schedule and display it in a nice formatted table.

This runs a hardcoded example (NETS major with CIS & NETS minors).

- Work Distribution

Arina: Data scraping and preprocessing — extracted real Penn major/minor requirements from department websites and normalized them into the flat rule-entry JSON format. Also handled course catalog data integration and validation.

Faig: Graph-based planning engine and scheduling algorithm — built the DAG representation, prerequisite resolution, topological sorting, TF-IDF matching, constraint satisfaction, and the greedy semester-filling logic. 

Both: Testing, debugging, and UI/UX refinement across the full pipeline. Iterated on the scheduling algorithm to handle edge cases like course equivalences, cascade blocking, and multi-minor constraints. Refined the chatbot to be intuitive and forgiving (case-insensitive inputs, normalization).

- Key Features

Handles multiple majors and minors with no double-counting within a program
Course equivalences (e.g., MATH 1400 ≡ MATH 1600)
Season availability tracking (Fall-only, Spring-only, every year vs. rotating)
Interest-based elective matching with TF-IDF
Automatic prerequisite collection
Warning system for unschedulable courses and missing data
Pretty terminal output with box-drawing UI

- Data Format

Courses, majors, and minors are stored as JSON:
- courses.json — Penn courses with prerequisites, credit units, seasons offered (scraped from Penn course catalog)
- majors.json — flat list of rule entries (majorName, sectionName, ruleType, options) scraped from department requirement pages
- minors.json — same structure for minors

The scraper extracts real Penn data and normalizes it into the flat rule-entry format. All files are configurable; just edit the JSON and re-run.

- AI usage

We have mainly used AI assistance for building the UI of our program. Apart from that, it has been used to scrape minors.json (though majors.json
and courses.json were NOT scraped using AI, we did that fully ourselves). The thing is, there are WAY too many minors and all of them have
different webpage structure, so we couldn't possible make a scraper for each minor in this period of time. However we did it for majors, and as
you can see, due to webpages having different structure, there is a lot of hardcoding.