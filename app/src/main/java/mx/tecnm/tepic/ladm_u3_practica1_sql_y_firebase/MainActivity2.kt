package mx.tecnm.tepic.ladm_u3_practica1_sql_y_firebase

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {

    var baseLocal = BaseDatos(this,"muebleria",null,1)
    var baseRemota = FirebaseFirestore.getInstance()
    var idSQL = ""
    var idNoSQL = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        var extra = intent.extras
        var status = extra!!.getBoolean("status")


        if (!status){
            idSQL = extra!!.getString("idElegido")!!
            cargarSQL(extra,idSQL)
        }else{
            idNoSQL = extra!!.getString("idElegido")!!
            cargarNoSQL(extra,idNoSQL)
        }

        button2.setOnClickListener {
            if (!status){
                actualizarSQL(idSQL)
            }else{
                actualizarNoSQL(idNoSQL)
            }
        }
        button4.setOnClickListener {
            finish()
        }


    }

    private fun actualizarNoSQL(idNoSQL: String) {
        baseRemota.collection("APARTADO")
                .document(idNoSQL)
                .update("nombre",actcliente.text.toString(),
                        "producto",actproducto.text.toString(),
                        "precio",actprecio.text.toString())
                .addOnSuccessListener {
                    alerta("EXITO SE ACTUALIZO")
                }
                .addOnFailureListener {
                    mensaje("ERROR NO SE PUDO ACTUALIZAR")
                }
    }

    private fun cargarNoSQL(extra: Bundle, idNoSQL: String) {
        baseRemota.collection("APARTADO")
                .document(idNoSQL)
                .get()
                .addOnSuccessListener {
                    actcliente.setText(it.getString("cliente"))
                    actproducto.setText(it.getString("producto"))
                    actprecio.setText(it.get("precio").toString())
                }
                .addOnFailureListener {
                    mensaje("ERROR: ${it.message!!}")
                }
    }

    private fun actualizarSQL(idSQL: String) {
        try {
            var transaccion = baseLocal.writableDatabase
            var valores = ContentValues()
            valores.put("NOMBRECLIENTE",actcliente.text.toString())
            valores.put("PRODUCTO",actproducto.text.toString())
            valores.put("PRECIO",actprecio.text.toString().toFloat())

            var resultado = transaccion.update("APARTADO",valores,"IDAPARTADO=?", arrayOf(idSQL))
            if (resultado > 0){
                mensaje("Se ACTUALIZO correctamente ID")
                finish()
            }else{
                mensaje("ERROR! no se ACTUALIZO")
            }
            transaccion.close()
        }catch (err:SQLiteException){
            mensaje(err.message!!)
        }
    }

    private fun cargarSQL(extra: Bundle, id: String) {

        try{
            var transaccion = baseLocal.readableDatabase
            var cursor = transaccion.query("APARTADO", arrayOf("NOMBRECLIENTE","PRODUCTO","PRECIO"),"IDAPARTADO=?",
                arrayOf(this.idSQL),null,null,null)
            if (cursor.moveToFirst()){
                actcliente.setText(cursor.getString(0))
                actproducto.setText(cursor.getString(1))
                actprecio.setText(cursor.getString(2))
            }else{
                mensaje("ERROR! NO SE PUDO RECUPERAR LA DATA DE ID ${this.idSQL}")
            }
            transaccion.close()
        }catch (err: SQLiteException){
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

    private fun alerta(s: String) {
        Toast.makeText(this,s, Toast.LENGTH_LONG).show()
    }
}