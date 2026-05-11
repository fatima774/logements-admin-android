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

// Classe qui gere la page des logements
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

    // Fonction appelee automatiquement quand la page se cree
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

        // Quand on clique longtemps sur un logement - propose de le supprimer
        listViewLogements.setOnItemLongClickListener { _, _, position, _ ->
            // Recupere l'identifiant du logement selectionne
            val logementId = logementsIds[position]

            // Affiche une boite de dialogue de confirmation
            AlertDialog.Builder(this)
                .setTitle("Supprimer le logement")
                .setMessage("Voulez-vous supprimer ce logement ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    deleteLogement(logementId, position)
                }
                .setNegativeButton("Annuler", null)
                .show()
            true
        }

        // Charge la liste des logements au demarrage
        loadLogements()
    }

    // Fonction qui charge tous les logements depuis l'API
    private fun loadLogements() {

        // Affiche le message de chargement
        textViewLoading.visibility = TextView.VISIBLE

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // Cree la requete HTTP GET vers la route logements
                val request = Request.Builder()
                    .url("$API_URL/logements")
                    .get()
                    .build()

                // Envoie la requete et recupere la reponse
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

    // Fonction qui supprime un logement via l'API
    private fun deleteLogement(logementId: Int, position: Int) {

        // Lance une tache en arriere-plan
        CoroutineScope(Dispatchers.IO).launch {

            try {
                // Cree la requete HTTP DELETE vers la route admin/logements/:id
                val request = Request.Builder()
                    .url("$API_URL/admin/logements/$logementId")
                    .delete()
                    .build()

                // Envoie la requete et recupere la reponse
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