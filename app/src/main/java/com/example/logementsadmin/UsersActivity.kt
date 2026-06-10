package com.example.logementsadmin

// Import pour les fonctionnalites de base d'une page Android
import androidx.appcompat.app.AppCompatActivity
// Import pour le bundle de donnees au demarrage
import android.os.Bundle
// Import pour les boutons
import android.widget.Button
// Import pour afficher du texte
import android.widget.TextView
// Import pour afficher une liste d'elements
import android.widget.ListView
// Import pour adapter les donnees a la liste
import android.widget.ArrayAdapter
// Import pour les boites de dialogue
import android.app.AlertDialog
// Import pour les coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Import pour les requetes HTTP
import okhttp3.OkHttpClient
import okhttp3.Request
// Import pour lire le JSON
import org.json.JSONArray

/*
 * UsersActivity
 * 
 * Cette classe affiche la liste de tous les utilisateurs enregistrés
 * dans l'application, et permet de les supprimer en appuyant longtemps dessus.
 * 
 * Son rôle :
 * - Charger la liste des utilisateurs depuis l'API du serveur
 * - Afficher chaque utilisateur avec son nom et email
 * - Permettre de supprimer un utilisateur avec un clic long
 * - Gérer le token JWT pour s'authentifier auprès du serveur
 * - Utiliser SharedPreferences pour stocker et récupérer le token
 */
class UsersActivity : AppCompatActivity() {

    // Declaration des elements de l'interface
    private lateinit var listViewUsers: ListView
    private lateinit var textViewLoading: TextView
    private lateinit var buttonBack: Button

    // Liste qui stocke les donnees des utilisateurs
    private val usersList = mutableListOf<String>()

    // Liste qui stocke les identifiants des utilisateurs
    private val usersIds = mutableListOf<Int>()

    // Adresse de l'API backend sur Render
    private val API_URL = "https://logement-backend.onrender.com"

    // Client HTTP pour faire les requetes vers l'API
    private val client = OkHttpClient()

    /*
     * onCreate() - Fonction appelée automatiquement au démarrage de la page
     * 
     * Cette fonction :
     * - Charge l'interface graphique
     * - Récupère les éléments (liste, message, bouton retour)
     * - Définit le comportement du clic long sur les utilisateurs pour les supprimer
     * - Lance le chargement de la liste des utilisateurs depuis l'API
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definit l'interface a afficher
        setContentView(R.layout.activity_users)

        // Recupere les elements de l'interface par leur identifiant
        listViewUsers = findViewById(R.id.listViewUsers)
        textViewLoading = findViewById(R.id.textViewLoading)
        buttonBack = findViewById(R.id.buttonBack)

        // Quand on clique sur retour - revient au dashboard
        buttonBack.setOnClickListener {
            finish()
        }

        // Quand l'utilisateur clique longtemps sur un utilisateur de la liste
        listViewUsers.setOnItemLongClickListener { _, _, position, _ ->
            // Récupérer le nom de l'utilisateur à la position cliquée
            val userName = usersList[position]
            // Récupérer l'ID de l'utilisateur (important pour le supprimer)
            val userId = usersIds[position]

            // Afficher une boîte de dialogue (popup) pour confirmer la suppression
            AlertDialog.Builder(this)
                .setTitle("Supprimer l'utilisateur")
                .setMessage("Voulez-vous supprimer $userName ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    // Si l'utilisateur clique sur "Supprimer", appeler la fonction deleteUser
                    deleteUser(userId, position)
                }
                .setNegativeButton("Annuler", null)  // Annuler la suppression
                .show()
            true  // Indiquer que le clic long a été traité
        }

        // Charge la liste des utilisateurs au demarrage
        loadUsers()
    }

    /*
     * loadUsers() - Charge la liste de tous les utilisateurs depuis l'API
     * 
     * Cette fonction :
     * - Récupére le token JWT sauvegardé dans SharedPreferences
     * - Envoie une requête GET au serveur avec le token dans le header "Authorization"
     * - Traite la réponse JSON pour extraire les infos de chaque utilisateur
     * - Affiche la liste dans l'interface
     */
    private fun loadUsers() {

        // Affiche le message de chargement
        textViewLoading.visibility = TextView.VISIBLE

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // ========== GESTION DU TOKEN JWT ==========
                // Avant de faire la requête, récupérer le token JWT sauvegardé lors de la connexion
                // Ce token prouve au serveur qu'on a le droit d'accéder à ces données
                val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                // ========== APPEL OkHttp ==========
                // Créer une requête HTTP GET pour charger les utilisateurs
                // OkHttpClient gère les connexions, les données réseau, etc.
                val request = Request.Builder()
                    .url("$API_URL/admin/users")  // Route du serveur qui retourne la liste des utilisateurs
                    // Ajouter le token dans le header Authorization (obligatoire pour cette API)
                    // Format : "Bearer <token>" (Bearer = type de token)
                    .addHeader("Authorization", "Bearer $token")
                    .get()  // Méthode GET (juste récupérer des données, pas en envoyer)
                    .build()

                // Envoyer la requête et attendre la réponse du serveur
                val response = client.newCall(request).execute()

                // Lit le contenu de la reponse
                val responseBody = response.body?.string() ?: "[]"

                // Convertit la reponse JSON en tableau
                val jsonArray = JSONArray(responseBody)

                // Vide les listes avant de les remplir
                usersList.clear()
                usersIds.clear()

                // Parcourt chaque utilisateur dans le tableau JSON
                for (i in 0 until jsonArray.length()) {
                    // Recupere l'objet JSON de l'utilisateur
                    val user = jsonArray.getJSONObject(i)

                    // Recupere les informations de l'utilisateur
                    val id = user.getInt("id_user")
                    val nom = user.getString("nom")
                    val prenom = user.getString("prenom")
                    val email = user.getString("email")

                    // Ajoute l'identifiant dans la liste des identifiants
                    usersIds.add(id)

                    // Ajoute les informations dans la liste d'affichage
                    usersList.add("$prenom $nom\n$email")
                }

                // Revient sur le thread principal pour modifier l'interface
                withContext(Dispatchers.Main) {

                    // Cache le message de chargement
                    textViewLoading.visibility = TextView.GONE

                    // Cree un adaptateur pour afficher la liste
                    val adapter = ArrayAdapter(
                        this@UsersActivity,
                        R.layout.list_item,
                        usersList
                    )

                    // Applique l'adaptateur a la liste
                    listViewUsers.adapter = adapter
                }

            } catch (e: Exception) {
                // En cas d'erreur
                withContext(Dispatchers.Main) {
                    textViewLoading.text = "Erreur : ${e.message}"
                }
            }
        }
    }

    /*
     * deleteUser() - Supprime un utilisateur du serveur
     * 
     * Cette fonction :
     * - Envoie une requête DELETE au serveur avec le token JWT
     * - Supprime l'utilisateur de la liste affichée si la suppression réussit
     * - Met à jour l'affichage de la liste
     */
    private fun deleteUser(userId: Int, position: Int) {

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // ========== GESTION DU TOKEN JWT ==========
                // Récupérer le token JWT sauvegardé lors de la connexion (voir LoginActivity)
                // Ce token est nécessaire pour prouver qu'on a le droit de supprimer un utilisateur
                val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                // ========== APPEL OkHttp ==========
                // Créer une requête HTTP DELETE pour supprimer l'utilisateur
                val request = Request.Builder()
                    .url("$API_URL/admin/users/$userId")  // Route du serveur avec l'ID de l'utilisateur à supprimer
                    // Ajouter le token pour prouver l'authentification
                    .addHeader("Authorization", "Bearer $token")
                    .delete()  // Méthode DELETE (supprimer une ressource)
                    .build()

                // Envoyer la requête et attendre la réponse du serveur
                val response = client.newCall(request).execute()

                // Revient sur le thread principal pour modifier l'interface
                withContext(Dispatchers.Main) {

                    // Verifie si la suppression est reussie
                    if (response.isSuccessful) {
                        // Supprime l'utilisateur de la liste affichee
                        usersList.removeAt(position)
                        usersIds.removeAt(position)

                        // Met a jour l'affichage de la liste
                        (listViewUsers.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                    } else {
                        textViewLoading.text = "Erreur lors de la suppression"
                        textViewLoading.visibility = TextView.VISIBLE
                    }
                }

            } catch (e: Exception) {
                // En cas d'erreur
                withContext(Dispatchers.Main) {
                    textViewLoading.text = "Erreur : ${e.message}"
                    textViewLoading.visibility = TextView.VISIBLE
                }
            }
        }
    }
}