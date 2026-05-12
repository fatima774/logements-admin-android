package com.example.logementsadmin

// Import pour la navigation entre pages
import android.content.Intent
// Import pour les fonctionnalites de base d'une page Android
import androidx.appcompat.app.AppCompatActivity
// Import pour le bundle de donnees au demarrage
import android.os.Bundle
// Import pour les boutons
import android.widget.Button
// Import pour les champs de texte
import android.widget.EditText
// Import pour afficher du texte
import android.widget.TextView
// Import pour les coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Import pour les requetes HTTP
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
// Import pour lire le JSON
import org.json.JSONObject

// Classe qui gere la page de connexion
class LoginActivity : AppCompatActivity() {

    // Declaration des elements de l'interface
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var textViewError: TextView

    // Compteur du nombre de tentatives de connexion echouees
    private var attempts = 0

    // Nombre maximum de tentatives autorisees avant blocage
    private val maxAttempts = 5

    // Adresse de l'API backend sur Render
    private val API_URL = "https://logement-backend.onrender.com"

    // Client HTTP pour faire les requetes vers l'API
    private val client = OkHttpClient()

    // Fonction appelee automatiquement quand la page se cree
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definit l'interface a afficher
        setContentView(R.layout.activity_login)

        // Recupere les elements de l'interface par leur identifiant
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewError = findViewById(R.id.textViewError)

        // Definit ce qui se passe quand on clique sur le bouton
        buttonLogin.setOnClickListener {
            // Recupere ce que l'utilisateur a tape
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            // Verifie que les champs ne sont pas vides
            if (username.isEmpty() || password.isEmpty()) {
                showError("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            // Lance la verification des identifiants
            verifyCredentials(username, password)
        }
    }

    // Fonction qui verifie les identifiants via l'API
    private fun verifyCredentials(username: String, password: String) {

        // Verifie si le nombre maximum de tentatives est atteint
        if (attempts >= maxAttempts) {
            showError("Trop de tentatives. Application bloquee.")
            buttonLogin.isEnabled = false
            return
        }

        // Desactive le bouton pendant la verification
        buttonLogin.isEnabled = false

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // Cree le corps de la requete en JSON
                val jsonBody = JSONObject()
                jsonBody.put("username", username)
                jsonBody.put("password", password)

                // Definit le type de contenu envoye
                val mediaType = "application/json".toMediaType()

                // Cree le corps de la requete
                val body = jsonBody.toString().toRequestBody(mediaType)

                // Cree la requete HTTP POST vers la route admin/login
                val request = Request.Builder()
                    .url("$API_URL/admin/login")
                    .post(body)
                    .build()

                // Envoie la requete et recupere la reponse
                val response = client.newCall(request).execute()

                // Lit le contenu de la reponse
                val responseBody = response.body?.string() ?: ""

                // Revient sur le thread principal pour modifier l'interface
                withContext(Dispatchers.Main) {

                    // Verifie si la connexion est reussie
                    if (response.isSuccessful) {
                        // Connexion reussie - navigue vers le dashboard
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()}
                    else {
                        // Connexion echouee - affiche le message d'erreur
                        attempts++
                        val errorJson = JSONObject(responseBody)
                        val errorMessage = errorJson.optString("error", "Identifiants incorrects")
                        showError("$errorMessage. Tentatives restantes : ${maxAttempts - attempts}")
                        buttonLogin.isEnabled = true
                    }
                }

            } catch (e: Exception) {
                // En cas d'erreur reseau
                withContext(Dispatchers.Main) {
                    showError("Erreur reseau : verifiez votre connexion")
                    buttonLogin.isEnabled = true
                }
            }
        }
    }

    // Fonction qui affiche un message d'erreur en rouge
    private fun showError(message: String) {
        textViewError.text = message
        textViewError.visibility = TextView.VISIBLE
    }
}