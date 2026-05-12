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

// Classe qui gere la page des utilisateurs
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

    // Fonction appelee automatiquement quand la page se cree
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

        // Quand on clique longtemps sur un utilisateur - propose de le supprimer
        listViewUsers.setOnItemLongClickListener { _, _, position, _ ->
            // Recupere le nom de l'utilisateur selectionne
            val userName = usersList[position]
            // Recupere l'identifiant de l'utilisateur selectionne
            val userId = usersIds[position]

            // Affiche une boite de dialogue de confirmation
            AlertDialog.Builder(this)
                .setTitle("Supprimer l'utilisateur")
                .setMessage("Voulez-vous supprimer $userName ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    deleteUser(userId, position)
                }
                .setNegativeButton("Annuler", null)
                .show()
            true
        }

        // Charge la liste des utilisateurs au demarrage
        loadUsers()
    }

    // Fonction qui charge tous les utilisateurs depuis l'API
    private fun loadUsers() {

        // Affiche le message de chargement
        textViewLoading.visibility = TextView.VISIBLE

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // Cree la requete HTTP GET vers la route admin/users
                val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val request = Request.Builder()
                    .url("$API_URL/admin/users")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                // Envoie la requete et recupere la reponse
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

    // Fonction qui supprime un utilisateur via l'API
    private fun deleteUser(userId: Int, position: Int) {

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // Recupere le token sauvegarde lors de la connexion
                val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

// Cree la requete HTTP DELETE vers la route admin/users/:id
                val request = Request.Builder()
                    .url("$API_URL/admin/users/$userId")
                    .addHeader("Authorization", "Bearer $token")
                    .delete()
                    .build()

                // Envoie la requete et recupere la reponse
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