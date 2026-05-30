# HapifyMe API End-to-End (E2E) Automation Project

[![HapifyMe API E2E Automation](https://github.com/andraelenaionascu-ops/Final_Project_E2E_Automation_CI-CD_Integration/actions/workflows/test.yml/badge.svg)](https://github.com/andraelenaionascu-ops/Final_Project_E2E_Automation_CI-CD_Integration/actions/workflows/test.yml)

Acest proiect reprezintă o suită de testare automatizată de tip **End-to-End (E2E)** pentru backend-ul aplicației **HapifyMe**, realizată ca parte a proiectului final de curs. Suita acoperă întregul ciclu de viață al unui utilizator, de la înregistrare până la ștergerea definitivă a contului, asigurând integritatea fluxurilor logice și a mecanismelor de securitate.

Proiectul include integrarea continuă (**CI/CD**) prin GitHub Actions, executând testele automat la fiecare modificare de cod.

## 🚀 Tehnologii Utilizate

* **Java 21** - Limbajul de programare principal.
* **REST Assured 5.5.0** - Framework-ul utilizat pentru interogarea endpoint-urilor HTTP și validarea răspunsurilor JSON.
* **TestNG 7.8.0** - Framework-ul de testare folosit pentru prioritizarea pașilor (`priority`) și gestionarea dependențelor între teste (`dependsOnMethods`).
* **Awaitility 4.2.0** - Utilizat pentru gestionarea așteptărilor asincrone (Polling API) la preluarea token-ului de confirmare.
* **Jackson/Groovy** - Pentru serializarea/deserializarea obiectelor de tip POJO și parsarea ierarhică a structurilor JSON.
* **GitHub Actions** - Platforma de CI/CD folosită pentru rularea pipeline-ului în cloud.

---

## 🔄 Fluxul de Testare E2E (8 Pași)

Suita execută în mod secvențial următorii pași corelați dinamic (State Sharing):

1.  **`testRegisterUser`** - Înregistrează un utilizator nou (`Andra QA`) folosind date generate aleatoriu. Extrage `api_key` și `user_id`.
2.  **`testGetConfirmationTokenAsync`** - Interoghează asincron backend-ul folosind **Awaitility** pentru a extrage token-ul de confirmare trimis pe email.
3.  **`testConfirmEmail`** - Activează contul proaspăt creat prin trimiterea token-ului ca Query Parameter.
4.  **`testLoginUser`** - Autentifică utilizatorul și extrage un **JWT Bearer Token** pentru sesiunile securizate.
5.  **`testGetProfileAndValidate`** - Verifică profilul nou creat. *Notă securitate: Folosește autentificarea prin `api_key` în header-ul Authorization și `user_id` ca query param.*
6.  **`testUpdateProfile`** - Modifică datele de profil (metoda `PUT`) și validează răspunsul serverului.
7.  **`testDeleteProfile`** - Șterge definitiv utilizatorul (metoda `DELETE`). *Notă securitate: Necesită în mod explicit formatul `Bearer <JWT_Token>` în header pentru validarea drepturilor destructive.*
8.  **`testNegativeProfileCheck`** - Încearcă re-accesarea profilului șters pentru a confirma distrugerea resursei (așteaptă status `401 Unauthorized` sau `404 Not Found`).

---

## 🛠️ Cum se rulează proiectul local

### Cerințe preliminare
* Java JDK 21 instalat.
* Maven configurat în variabilele de mediu.

### Rulare din Terminal
Pentru a executa întreaga suită de teste din linia de comandă, navighează în folderul rădăcină al proiectului și rulează:

```bash
mvn clean test

<img width="1406" height="277" alt="Github_Actions_Result" src="https://github.com/user-attachments/assets/2de7aaca-50ec-4b54-97bd-7fc27190c2c4" />
