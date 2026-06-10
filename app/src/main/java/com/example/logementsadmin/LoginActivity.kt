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

/*
 * LoginActivity
 * 
 * Cette classe gère la page de connexion de l'application.
 * C'est la première page que l'utilisateur voit en lançant l'app.
 * 
 * Son rôle : 
 * - Afficher un formulaire avec un champ pour le nom d'utilisateur et le mot de passe
 * - Vérifier les identifiants en envoyant une requête à l'API du serveur
 * - Sauvegarder le token JWT reçu du serveur pour les futures requêtes
 * - Bloquer l'accès après 5 tentatives échouées
 * - Rediriger vers le Dashboard si la connexion réussit
 */
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

    /*
     * onCreate() - Fonction appelée automatiquement au démarrage de la page
     * 
     * Cette fonction est exécutée une seule fois quand l'utilisateur ouvre l'app.
     * Elle configure :
     * - L'interface avec les champs de texte et boutons
     * - L'événement "clique" du bouton de connexion
     */
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

    /*
     * verifyCredentials() - Vérifie le nom d'utilisateur et le mot de passe auprès du serveur
     * 
     * Cette fonction :
     * - Envoie une requête HTTP POST (avec OkHttpClient) à l'API du serveur
     * - Inclut le nom d'utilisateur et le mot de passe dans le corps de la requête
     * - Récupère le token JWT du serveur s'il valide l'accès
     * - Sauvegarde le token dans SharedPreferences pour le réutiliser plus tard
     * - Compte les tentatives échouées et bloque après 5 essais
     */
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
                // ========== APPEL OkHttp ==========
                // 1. Créer le corps de la requête en JSON
                val jsonBody = JSONObject()
                jsonBody.put("username", username)
                jsonBody.put("password", password)

                // 2. Définir le type MIME (JSON dans ce cas)
                val mediaType = "application/json".toMediaType()

                // 3. Convertir le JSON en corps de requête
                val body = jsonBody.toString().toRequestBody(mediaType)

                // 4. Créer la requête HTTP POST vers le serveur
                // OkHttpClient est un client HTTP très populaire en Android
                val request = Request.Builder()
                    .url("$API_URL/admin/login")  // Envoyer vers la route /admin/login du serveur
                    .post(body)  // Méthode POST avec le corps JSON
                    .build()

                // Envoyer la requête et attendre la réponse du serveur
                val response = client.newCall(request).execute()

                // Lit le contenu de la reponse
                val responseBody = response.body?.string() ?: ""

                // Revient sur le thread principal pour modifier l'interface
                withContext(Dispatchers.Main) {

                    // Vérifier si le serveur a accepté les identifiants (code 200, 201, etc.)
                    if (response.isSuccessful) {
                        // ========== GESTION DU TOKEN JWT ==========
                        // 1. Extraire le token du serveur (il est en JSON dans la réponse)
                        val json = JSONObject(responseBody)
                        val token = json.optString("token", "")
                        
                        // ========== SAUVEGARDE AVEC SharedPreferences ==========
                        // SharedPreferences est un système simple pour sauvegarder des données locales
                        // dans le téléphone. C'est parfait pour les petites données comme un token JWT.
                        // 
                        // Le token JWT contient les informations de l'utilisateur connecté.
                        // On le sauvegarde pour l'ajouter à TOUTES les futures requêtes HTTP
                        // (voir UsersActivity et LogementsActivity où on fait : addHeader("Authorization", "Bearer $token")
                        val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                        prefs.edit().putString("token", token).apply()  // .apply() sauvegarde le token

                        // Connexion reussie - navigue vers le dashboard
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
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

    /*
     * showError() - Affiche un message d'erreur à l'écran
     * 
     * Cette fonction affiche le message en rouge pour montrer à l'utilisateur
     * que quelque chose s'est mal passé (mauvais identifiant, problème réseau, etc.)
     */
    private fun showError(message: String) {
        textViewError.text = message
        textViewError.visibility = TextView.VISIBLE
    }
}