package mx.tecnm.tepic.ladm_u3_practica1_sql_y_firebase

import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var baseLocal = BaseDatos(this,"muebleria",null,1)
    var baseRemota = FirebaseFirestore.getInstance()
    var listaID = ArrayList<String>()
    var listaIDNoSQL = ArrayList<String>()
    var dataLista = ArrayList<String>()
    var dataListaNoSQL = ArrayList<String>()
    var status = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //CARGAR LISTVIEWS
        cargarApartados()


        if (listaID.isEmpty()){
            button2.isEnabled = false
        }

        button.setOnClickListener {
            insertarSQL()
        }
        button2.setOnClickListener {
            sincronizar()
        }

    }

    private fun sincronizar() {
        try {
            //VARIABLES NO SQL
            var cliente = ""
            var producto = ""
            var precio = 0f

            //VARIABLES SQL
            var select = baseLocal.readableDatabase
            var SQL = "SELECT * FROM APARTADO"

            //OBTENER TODOS LOS DATOS DE LA BD SQL
            var cursor = select.rawQuery(SQL,null)

            if (cursor.moveToFirst()){
                do {
                    cliente = cursor.getString(1)
                    producto = cursor.getString(2)
                    precio = cursor.getString(3).toFloat()

                    var datosInsertar = hashMapOf(
                        "cliente" to cliente,
                        "producto" to producto,
                        "precio" to precio
                    )

                    baseRemota.collection("APARTADO")
                        .add(datosInsertar)
                        .addOnSuccessListener {
                            alerta("SE INSERTO CORRECTAMENTE EN LA NUBE")
                            limpiarCampos()
                        }
                        .addOnFailureListener{
                            mensaje("ERROR: ${it.message!!}")
                        }

                }while (cursor.moveToNext())
            }else{
                mensaje("NO HAY APARTADOS QUE SINCRONIZAR")
            }
            eliminarTodosSQL()
            status = true;
            cargarApartadosNoSQL()
            button2.isEnabled = false
            select.close()
        }catch (err:SQLiteException){
            mensaje(err.message!!)
        }


    }

    private fun eliminarTodosSQL() {
        try {
            var eliminar = baseLocal.writableDatabase
            var SQL = "DELETE FROM APARTADO"
            eliminar.execSQL(SQL)
            cargarApartados()
            eliminar.close()
        }catch (err:SQLiteException){
            mensaje(err.message!!)
        }
    }

    private fun cargarApartados() {
        try {
            var select = baseLocal.readableDatabase
            var apartados = ArrayList<String>()
            var SQL = "SELECT * FROM APARTADO"

            var cursor = select.rawQuery(SQL,null)
            listaID.clear()
            if (cursor.moveToFirst()){
                do {
                    var data = "[ "+cursor.getString(1)+"]  -- "+cursor.getString(2)+" --- Precio: "+cursor.getString(3)
                    apartados.add(data)
                    listaID.add(cursor.getInt(0).toString())
                }while (cursor.moveToNext())
            }else{
                apartados.add("NO HAY APARTADOS")
            }
            select.close()
            listalocal.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,apartados)
            listalocal.setOnItemClickListener { parent, view, position, id ->
                var idBorrar = listaID.get(position)
                AlertDialog.Builder(this)
                    .setTitle("ATENCION")
                    .setMessage("QUE DESEAS HACER CON: "+idBorrar)
                    .setNegativeButton("CANCELAR"){d,i->}
                    .setPositiveButton("ELIMINAR"){d,i->
                        eliminarSQL(idBorrar)
                    }
                    .setNeutralButton("ACTUALIZAR"){d,i->
                        var intent = Intent(this,MainActivity2::class.java)
                        intent.putExtra("idElegido",idBorrar)
                        intent.putExtra("status",status)
                        startActivity(intent)
                    }
                    .show()
            }
        }catch (err:SQLiteException){
            mensaje(err.message!!)
        }
    }

    private fun alerta(s: String) {
        Toast.makeText(this,s, Toast.LENGTH_LONG).show()
    }

    private fun cargarApartadosNoSQL() {
        baseRemota.collection("APARTADO").addSnapshotListener{ querySnapshot, error ->

            if(error != null){
                mensaje(error.message!!)
                return@addSnapshotListener
            }

            dataListaNoSQL.clear()
            listaIDNoSQL.clear()

            for (document in querySnapshot!!){
                var cadena = "[${document.getString("cliente")}] --- ${document.get("producto")} --- Precio: ${document.get("precio")}"
                dataListaNoSQL.add(cadena)
                listaIDNoSQL.add(document.id.toString())
            }
            listafirebase.adapter = ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,dataListaNoSQL)
            listafirebase.setOnItemClickListener { parent, view, position, id ->
                dialogoEliminaActualiza(position)
            }
        }
    }

    private fun dialogoEliminaActualiza(position: Int) {
        var idElegido = listaIDNoSQL.get(position)
        AlertDialog.Builder(this).setTitle("ATENCION!!")
                .setMessage("QUE DESEAS HACER CON \n ${dataListaNoSQL.get(position)}?")
                .setPositiveButton("ELIMINAR"){d, i->
                    eliminarNoSQL(idElegido)
                }
                .setNeutralButton("ACTUALIZAR"){d,i->
                    var intent = Intent(this,MainActivity2::class.java)
                    intent.putExtra("idElegido",idElegido)
                    intent.putExtra("status",status)
                    startActivity(intent)
                }
                .setNegativeButton("CANCELAR"){d,i->}
                .show()
    }

    private fun eliminarNoSQL(idElegido: String) {
        baseRemota.collection("APARTADO")
            .document(idElegido)
            .delete()
            .addOnFailureListener {
                mensaje("ERROR! ${it.message!!}")
            }
            .addOnSuccessListener {
                mensaje("SE ELIMINO CON EXITO")
            }
    }

    private fun insertarSQL() {
        try {
            var insertar = baseLocal.writableDatabase
            var SQL = "INSERT INTO APARTADO VALUES(NULL,'${cliente.text.toString()}','${producto.text.toString()}','${precio.text.toString().toFloat()}')"
            insertar.execSQL(SQL)
            cargarApartados()
            limpiarCampos()
            insertar.close()
        }catch (err: SQLiteException){
            mensaje(err.message!!)
        }
        button2.isEnabled = true
    }

    private fun limpiarCampos() {
        cliente.setText("")
        producto.setText("")
        precio.setText("")
    }

    private fun eliminarSQL(idBorrar: String) {
        try {
            var eliminar = baseLocal.writableDatabase
            var SQL = "DELETE FROM APARTADO WHERE IDAPARTADO = ${idBorrar}"
            eliminar.execSQL(SQL)
            cargarApartados()
            eliminar.close()
        }catch (err:SQLiteException){
            mensaje(err.message!!)
        }
    }

    private fun mensaje(s: String) {
        AlertDialog.Builder(this)
            .setTitle("ATENCION")
            .setMessage(s)
            .setPositiveButton("OK"){
                    d,i->
            }
            .show()
    }
    
}