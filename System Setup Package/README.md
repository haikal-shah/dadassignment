# MasakGramPrompt: Nutritional LLM Analysis Dashboard
*BITP 3123 - Sem 2 2025/2026*

Welcome to **MasakGramPrompt**, a distributed Java TCP/IP system that analyses Malaysian Instagram cooking reels using Local LLMs via LangChain4j and Ollama. This guide contains everything you need to set up the system on a completely new computer, from database initialisation to running the application.

---

## 🏗️ 1. System Requirements
Before starting, ensure your computer has the following software installed:
1. **Java Development Kit (JDK 17 or newer)**
2. **Eclipse IDE for Enterprise Java Developers**
3. **Apache Maven** (Integrated into Eclipse)
4. **MySQL Server & MySQL Workbench**
5. **Ollama** (For running local LLMs without API keys)

---

## 🗄️ 2. Database Setup (MySQL Workbench)

The system relies on a MySQL database to store reels, transcripts, and LLM experiment results. 

1. Open **MySQL Workbench** and connect to your local MySQL Server.
2. Open a new SQL tab.
3. Create the database:
   ```sql
   CREATE DATABASE masakgramprompt;
   ```
4. Import the schema and seed the real data. Open the following SQL scripts in MySQL Workbench and execute them in this exact order:
   - `create_tables.sql`
   - `seed_data.sql`
   - `migrate_schema.sql`
   - `seed_real_data.sql`
   > **Note:** `seed_real_data.sql` inserts 10 real Instagram reels from influencer **Danish Harraz** along with their transcript file mappings.

---

## 🧠 3. Local LLM Setup (Ollama)

This project uses local AI models to ensure privacy and zero API costs. The `LLMService` connects to Ollama on `localhost:11434`.

1. Download and install Ollama from [ollama.com](https://ollama.com/).
2. Open a terminal/command prompt and pull the models required for the system. *Note: You only need to pull the models you plan to test, but Llama 3.2 is highly recommended as a baseline.*
   ```bash
   ollama pull llama3.2:3b
   ollama pull phi4-mini
   ollama pull qwen2.5:3b
   ollama pull medgemma:4b
   ```
3. Ensure the Ollama app is running in your system tray before starting the Java server.

---

## 💻 4. Importing the Code to Eclipse

The system is split into four Maven projects. You must import all four into your Eclipse workspace.

1. Open **Eclipse IDE**.
2. Go to **File -> Import...**
3. Select **Maven -> Existing Maven Projects** and click Next.
4. Click **Browse** and select the folder `test/` (which contains the 3 project folders).
5. Ensure the following four projects are checked:
   - masakgramprompt-common
   - nutritional-llm-service
   - masakgramprompt-server
   - masakgramprompt-client
6. Click **Finish**.
7. *Important:* Right-click each project -> **Maven -> Update Project...** -> Check "Force Update of Snapshots/Releases" -> Click OK.
8. Go to **Project -> Clean...** and clean all projects to ensure fresh `.class` files.

---

## 🚀 5. Running the System

Because this is a distributed TCP/IP architecture, you must start the Server first, followed by the Client.

### Step 1: Start the Server
1. Expand the `masakgramprompt-server` project.
2. Navigate to `src/main/java/edu/utem/ftmk/server/ServerMain.java`.
3. Right-click `ServerMain.java` -> **Run As -> Java Application**.
4. You should see console output saying the server is listening on Port `12345`.

### Step 2: Start the Client
1. Expand the `masakgramprompt-client` project.
2. Navigate to `src/main/java/edu/utem/ftmk/client/ClientMain.java`.
3. Right-click `ClientMain.java` -> **Run As -> Java Application**.
4. The **MasakGramPrompt Dashboard** will launch!

---

## 📊 6. How to Use the Dashboard

Once the dashboard is running, you will see 4 distinct tabs:

### Tab 1: Reel Analysis
- View the 10 seeded reels from Danish Harraz.
- Click any reel to preview its transcript. **Malay words are automatically highlighted in yellow** to showcase the code-switching nature of the data.
- The Status Matrix at the bottom shows which models and techniques have already been run for the selected reel.

### Tab 2: Run Experiment
- Select a Reel and an Ollama LLM Model.
- Select one or more **Prompt Techniques** (e.g., zero-shot, few-shot).
- Click **▶ Run Selected Experiments**.
- Watch the real-time status grid update (`pending` -> `running` -> `completed`) as the server communicates with Ollama.

### Tab 3: Results Viewer
- Select any `completed` experiment from the dropdown and click **Load Result**.
- View the **Nutritional Fact Sheet** comparing LLM predictions against ground truth.
- The Ingredients Extracted table flags any **hallucinated ingredients in Red**.
- Click **⬇ Download CSV** to export the fact sheet for that specific experiment.

### Tab 4: Data Export
- This tab manages the 5 evaluation layers required by the project specification (Layer 1A to Layer 5).
- Click any of the 10 metric buttons to **Preview** the data in the grid.
- Click **⬇ Download Displayed CSV** to export the previewed metric to your local folder for your final report.

---

### Troubleshooting
- **Connection Refused:** Ensure `ServerMain` is running before `ClientMain`.
- **Database Errors (No suitable driver):** Ensure `mysql-connector-j` is correctly loaded via Maven pom.xml and your MySQL Server is running.
- **LLM Connection Timeout:** Ensure Ollama is running (`http://localhost:11434` should respond) and you have pulled the requested model.

---

## 🔄 7. Upgrading / Existing Installations

If the computer already has a previous version of this system installed:

1. **Existing Database:** The `create_tables.sql` script might fail or cause duplicates if the tables already exist. To do a clean install, first run `DROP DATABASE masakgramprompt;` in MySQL Workbench, then follow Step 2.
2. **Existing Java Projects:** If you already imported an older version of `masakgramprompt-server` into Eclipse, you must delete the old projects from your Eclipse workspace first (*Right click -> Delete -> Check "Delete project contents on disk"*), or simply switch to a brand new Eclipse Workspace (*File -> Switch Workspace*) before importing these new Java files.
