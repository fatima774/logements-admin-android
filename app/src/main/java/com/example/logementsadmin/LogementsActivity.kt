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
 * LogementsActivity
 * 
 * Cette classe affiche la liste de tous les logements (appartements, maisons, etc.)
 * enregistrés dans l'application, et permet de les supprimer en appuyant longtemps dessus.
 * 
 * Son rôle :
 * - Charger la liste des logements depuis l'API du serveur
 * - Afficher chaque logement avec son titre, sa ville et son prix
 * - Permettre de supprimer un logement avec un clic long
 * - Gérer le token JWT pour s'authentifier auprès du serveur
 * - Utiliser SharedPreferences pour stocker et récupérer le token
 */
class LogementsActivity : AppCompatActivity() {

    // Declaration des elements de l'interface
    private lateinit var listViewLogements: ListView
    private lateinit var textViewLoading: TextView
    private lateinit var buttonBack: Button

    // Liste qui stocke les donnees des logements
    private val logementsList = mutableListOf<String>()

    // Liste qui stocke les identifiants des logements
    private val logementsIds = mutableListOf<Int>()

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
     * - Définit le comportement du clic long sur les logements pour les supprimer
     * - Lance le chargement de la liste des logements depuis l'API
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definit l'interface a afficher
        setContentView(R.layout.activity_logements)

        // Recupere les elements de l'interface par leur identifiant
        listViewLogements = findViewById(R.id.listViewLogements)
        textViewLoading = findViewById(R.id.textViewLoading)
        buttonBack = findViewById(R.id.buttonBack)

        // Quand on clique sur retour - revient au dashboard
        buttonBack.setOnClickListener {
            finish()
        }

        // Quand l'utilisateur clique longtemps sur un logement de la liste
        listViewLogements.setOnItemLongClickListener { _, _, position, _ ->
            // Récupérer l'ID du logement à la position cliquée (important pour le supprimer)
            val logementId = logementsIds[position]

            // Afficher une boîte de dialogue (popup) pour confirmer la suppression
            AlertDialog.Builder(this)
                .setTitle("Supprimer le logement")
                .setMessage("Voulez-vous supprimer ce logement ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    // Si l'utilisateur clique sur "Supprimer", appeler la fonction deleteLogement
                    deleteLogement(logementId, position)
                }
                .setNegativeButton("Annuler", null)  // Annuler la suppression
                .show()
            true  // Indiquer que le clic long a été traité
        }

        // Charge la liste des logements au demarrage
        loadLogements()
    }

    /*
     * loadLogements() - Charge la liste de tous les logements depuis l'API
     * 
     * Cette fonction :
     * - Envoie une requête GET au serveur (sans token, car la liste est publique)
     * - Traite la réponse JSON pour extraire les infos de chaque logement
     * - Affiche la liste dans l'interface
     */
    private fun loadLogements() {

        // Affiche le message de chargement
        textViewLoading.visibility = TextView.VISIBLE

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // ========== APPEL OkHttp SANS TOKEN ==========
                // Cette route retourne la liste publique des logements (pas besoin d'authentification)
                // OkHttpClient gère les connexions, les données réseau, etc.
                val request = Request.Builder()
                    .url("$API_URL/logements")  // Route publique du serveur qui retourne les logements
                    .get()  // Méthode GET (juste récupérer des données)
                    .build()

                // Envoyer la requête et attendre la réponse du serveur
                val response = client.newCall(request).execute()

                // Lit le contenu de la reponse
                val responseBody = response.body?.string() ?: "[]"

                // Convertit la reponse JSON en tableau
                val jsonArray = JSONArray(responseBody)

                // Vide les listes avant de les remplir
                logementsList.clear()
                logementsIds.clear()

                // Parcourt chaque logement dans le tableau JSON
                for (i in 0 until jsonArray.length()) {
                    // Recupere l'objet JSON du logement
                    val logement = jsonArray.getJSONObject(i)

                    // Recupere les informations du logement
                    val id = logement.getInt("id_logement")
                    val titre = logement.getString("titre")
                    val ville = logement.optString("ville", "Ville non precisee")
                    val prix = logement.optString("prix", "0")

                    // Ajoute l'identifiant dans la liste des identifiants
                    logementsIds.add(id)

                    // Ajoute les informations dans la liste d'affichage
                    logementsList.add("$titre\n$ville - $prix EUR")
                }

                // Revient sur le thread principal pour modifier l'interface
                withContext(Dispatchers.Main) {

                    // Cache le message de chargement
                    textViewLoading.visibility = TextView.GONE

                    // Cree un adaptateur pour afficher la liste
                    val adapter = ArrayAdapter(
                        this@LogementsActivity,
                        R.layout.list_item,
                        logementsList
                    )

                    // Applique l'adaptateur a la liste
                    listViewLogements.adapter = adapter
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
     * deleteLogement() - Supprime un logement du serveur
     * 
     * Cette fonction :
     * - Récupér le token JWT sauvegardé dans SharedPreferences
     * - Envoie une requête DELETE au serveur avec le token JWT
     * - Supprime le logement de la liste affichée si la suppression réussit
     * - Met à jour l'affichage de la liste
     */
    private fun deleteLogement(logementId: Int, position: Int) {

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // ========== GESTION DU TOKEN JWT ==========
                // Récupérer le token JWT sauvegardé lors de la connexion (voir LoginActivity)
                // Ce token prouve au serveur qu'on est identifié et qu'on a le droit de supprimer un logement
                val prefs = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                // ========== APPEL OkHttp ==========
                // Créer une requête HTTP DELETE pour supprimer le logement
                val request = Request.Builder()
                    .url("$API_URL/admin/logements/$logementId")  // Route du serveur avec l'ID du logement à supprimer
                    // Ajouter le token pour prouver l'authentification
                    // Format : "Bearer <token>" (Bearer = type de token, JWT = format du token)
                    .addHeader("Authorization", "Bearer $token")
                    .delete()  // Méthode DELETE (supprimer une ressource)
                    .build()
                // Envoyer la requête et attendre la réponse du serveur
                val response = client.newCall(request).execute()

                // Revient sur le thread principal pour modifier l'interface
                withContext(Dispatchers.Main) {

                    // Verifie si la suppression est reussie
                    if (response.isSuccessful) {
                        // Supprime le logement de la liste affichee
                        logementsList.removeAt(position)
                        logementsIds.removeAt(position)

                        // Met a jour l'affichage de la liste
                        (listViewLogements.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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