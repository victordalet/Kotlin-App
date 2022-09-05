package fr.jockeur.naturecollection

import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import fr.jockeur.naturecollection.PlantRepository.Singleton.databaseRef
import fr.jockeur.naturecollection.PlantRepository.Singleton.downloadUri
import fr.jockeur.naturecollection.PlantRepository.Singleton.plantList
import fr.jockeur.naturecollection.PlantRepository.Singleton.storageReference
import java.util.*

class PlantRepository {

    object Singleton{

        // donner le lien pour acceder au bucket
        private val BUCKET_URL = "gs://nature-collection-8f158.appspot.com"

        // se connecter à l'espace de stockage
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(BUCKET_URL)

        // se connecter à la référence "plants"
        val databaseRef = FirebaseDatabase.getInstance().getReference("plants")

        // créer la liste des plantes
        val plantList = arrayListOf<PlantModel>()

        // contenir le lien de l'image courante
        var downloadUri: Uri? = null
    }

    fun updateData(callback: () -> Unit){
        // absorber les données depuis la databaseReference
        databaseRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                // retirer les anciennes
                plantList.clear()
                // récolter la liste
                for (ds in snapshot.children){
                    // construire un objet plante
                    val plant = ds.getValue(PlantModel::class.java)

                    // vérifier que la plante n'est pas null
                    if (plant != null){
                        // ajouter la plante à notre liste
                        plantList.add(plant)
                    }
                }
                // actionner le callback
                callback()
            }

            override fun onCancelled(error: DatabaseError) {}

        })
    }

    //créer une fonction pour envoyer des fichiers sur le storage
    fun uploadImage(file: Uri, callback: () -> Unit) {
        // vérifier que le fichier n'est pas null
        if (file != null){
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val ref = storageReference.child(fileName)
            val uploadTask = ref.putFile(file)

            Singleton

            // démarer la tâche d'envoie
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->

                // si il y a eu un problème lors de l'envoie du fichier
                if (!task.isSuccessful){
                    task.exception?.let { throw it }
                }
                    return@Continuation ref.downloadUrl
            }).addOnCompleteListener{ task ->
                // vérifier si tout va bien fonctioner
                if (task.isSuccessful){
                    // récupérer l'image
                    downloadUri = task.result
                    callback()
                }
            }
        }
    }

    // mettre à jour l'objet plante en bdd
    fun updatePlant(plant: PlantModel) = databaseRef.child(plant.id).setValue(plant)

    // inserer une nouvelle planter en bdd
    fun insertPlant(plant: PlantModel) = databaseRef.child(plant.id).setValue(plant)

    // supprimer une plante de la bdd
    fun deletePlant(plant: PlantModel) = databaseRef.child(plant.id).removeValue()

}