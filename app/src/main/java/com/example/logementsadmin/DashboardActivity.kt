package com.example.logementsadmin

// Import pour la navigation entre pages
import android.content.Intent
// Import pour les fonctionnalites de base d'une page Android
import androidx.appcompat.app.AppCompatActivity
// Import pour le bundle de donnees au demarrage
import android.os.Bundle
// Import pour les boutons
import android.widget.Button

/*
 * DashboardActivity
 * 
 * Cette classe affiche le menu principal de l'application.
 * L'utilisateur arrive ici après s'être connecté avec succès.
 * 
 * Son rôle :
 * - Afficher 3 boutons : "Gerer les utilisateurs", "Gerer les logements", "Se deconnecter"
 * - Naviguer vers UsersActivity quand l'utilisateur clique sur "Utilisateurs"
 * - Naviguer vers LogementsActivity quand l'utilisateur clique sur "Logements"
 * - Retourner à LoginActivity et effacer l'historique quand l'utilisateur clique sur "Se deconnecter"
 */
class DashboardActivity : AppCompatActivity() {

    // Declaration des boutons de l'interface
    private lateinit var buttonUsers: Button
    private lateinit var buttonLogements: Button
    private lateinit var buttonLogout: Button

    /*
     * onCreate() - Fonction appelée automatiquement au démarrage de la page
     * 
     * Cette fonction :
     * - Charge l'interface graphique
     * - Récupère les 3 boutons du layout
     * - Définit ce qui se passe quand l'utilisateur clique sur chaque bouton
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definit l'interface a afficher
        setContentView(R.layout.activity_dashboard)

        // Recupere les boutons par leur identifiant
        buttonUsers = findViewById(R.id.buttonUsers)
        buttonLogements = findViewById(R.id.buttonLogements)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Quand on clique sur le bouton "Gerer les utilisateurs"
        // Créer un Intent (c'est un "ordre" pour aller à une autre page)
        buttonUsers.setOnClickListener {
            val intent = Intent(this, UsersActivity::class.java)
            // Intent.FLAG_ACTIVITY_NEW_TASK : crée une nouvelle "stack" de pages
            // Intent.FLAG_ACTIVITY_CLEAR_TASK : efface l'historique précédent
            // Lancer la page des utilisateurs
            startActivity(intent)
        }

        // Quand on clique sur le bouton "Gerer les logements"
        buttonLogements.setOnClickListener {
            // Créer un Intent pour aller à la page des logements
            val intent = Intent(this, LogementsActivity::class.java)
            // Lancer la page des logements
            startActivity(intent)
        }

        // Quand on clique sur le bouton "Se deconnecter"
        // Déconnecter l'utilisateur en revenant à LoginActivity
        buttonLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            // Important : Effacer tout l'historique de navigation
            // Comme ça, l'utilisateur ne peut pas faire "retour" pour revenir au Dashboard sans se reloger
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Lancer la page de connexion
            startActivity(intent)
        }
    }
}