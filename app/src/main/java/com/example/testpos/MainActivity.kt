package com.example.testpos

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.system.OsConstants.ECONNREFUSED
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.testpos.database.transaction.*
import com.example.testpos.evenbus.data.MessageEvent
import com.imohsenb.ISO8583.builders.ISOClientBuilder
import com.imohsenb.ISO8583.builders.ISOMessageBuilder
import com.imohsenb.ISO8583.entities.ISOMessage
import com.imohsenb.ISO8583.enums.FIELDS
import com.imohsenb.ISO8583.enums.MESSAGE_FUNCTION
import com.imohsenb.ISO8583.enums.MESSAGE_ORIGIN
import com.imohsenb.ISO8583.enums.VERSION
import com.imohsenb.ISO8583.exceptions.ISOClientException
import com.imohsenb.ISO8583.exceptions.ISOException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

        var appDatabase : AppDatabase? = null
        var reversalDAO : ReversalDao? = null
        var saleDAO : SaleDao? = null

        var amount: String? = null
        var addAmount: EditText? = null
        var btnSetAmount: Button? = null
        var btnConnect: Button? = null
        var output1: TextView? = null
        var output2: TextView? = null
        var stan: Int? = null
        var reverseFlag = false
        var reversal: String? = null
        var responseCode: String? = null
        var reReversal: String? = null
        var reversalMsg: ISOMessage? = null
        var saleMsg: ISOMessage? = null
        var readSale: String? = null
        var readStan: Int? = null
        var stuckReverse = false

        private val HOST = "192.168.43.24"
//        private val HOST = "192.168.233.93";
//        private val HOST = "192.168.68.165";
        var PORT = 3000
//        private val HOST = "192.168.43.195"
    //    private val String HOST = "192.168.223.187";
    //    private val HOST = "192.168.68.185"
//        var PORT = 5000


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            addAmount = findViewById(R.id.addAmount)
            btnSetAmount = findViewById(R.id.setAmount)
            btnConnect = findViewById(R.id.btnConnect)
            output1 = findViewById(R.id.textViewRequest)
            output2 = findViewById(R.id.textViewResponse)

//

            btnConnect?.setOnClickListener{

//                 check reversal
                if(reverseFlag){
                    stuckReverse = true

                    Log.i("log_tag", "send reverse packet")
//                    sendPacket(reversalPacket(stan.toString()))
                    sendPacket(reBuildISOPacket(reReversal.toString()))
                    Log.i("log_tag", "reversal:  " + reReversal.toString())
                    Log.i("log_tag", "reverseFlag:  " + reverseFlag)

//                    runOnUiThread {
////                        Toast.makeText(applicationContext,"Time out!!",Toast.LENGTH_LONG).show()
////                        output2?.setText("stan " + stan);
//                        sendReversal()
//                    }

//                    Thread{
//
//                        accessDatabase()
//
//                        reversalDAO?.insertReversal(reverseTrans)
////                        startActivity(Intent(this,ReversalList::class.java))
//                        reReversal = reversalDAO?.getReversal()?.isoMsg
//                        Log.i("log_tag","reReversal:  " + reReversal.toString() )
//
//                    }.start()

                } else {
                    stan = stan?.plus(1)
                    saleMsg = salePacket(stan.toString())
                    Log.i("log_tag", "Current stan: " + stan)

                    reversalMsg = reversalPacket(stan.toString())
                    var reverseTrans = ReversalEntity(null,reversalMsg.toString())

                    reverseFlag = true
                    Log.i("log_tag", "send sale packet")
                    sendPacket(saleMsg)
                    Log.i("log_tag", "sale: " + saleMsg.toString())
                    Log.i("log_tag", "reverseFlag:  " + reverseFlag)
    //                Log.i("log_tag", "else" + reverseFlag)

                    runOnUiThread {
                        output2?.setText("Sale packet:  " + saleMsg.toString())
                    }

                    Thread{

                        accessDatabase()

                        reversalDAO?.insertReversal(reverseTrans)
//                        startActivity(Intent(this,ReversalList::class.java))
                        reReversal = reversalDAO?.getReversal()?.isoMsg
//                        Log.i("log_tag","reReversal:  " + reReversal.toString() )

                    }.start()

                }

            }

            btnSetAmount?.setOnClickListener{
                amount = addAmount?.getText().toString()
    //            output1?.setText("Amont : " + amount);
    //            Toast.makeText(applicationContext,"Time out!!",Toast.LENGTH_SHORT).show()

                Thread{
                        accessDatabase()
                        readStan = saleDAO?.getSale()?.STAN

                 }.start()

//                Log.i("log_tag","readSTAN : " + readStan)

                if(readStan == null){
                    stan = 1117
                }

                output1?.setText("Amount: " + amount)
                Log.d("log_tag", "input amount :  " + amount)
                Log.i("log_tag", "Previous stan :  " + stan)
            }

    //        Thread{
    //            val db = Room.databaseBuilder(
    //                applicationContext,AppDatabase::class.java,"appDB"
    //            ).build()
    //
    //            Log.i("SQLiteDebug","write")
    //            val reversal = db.reversalDao()
    //            val item = ReversalEntity("0","123456")
    //
    //            reversal.insertAllReversal(item)
    //
    //            Log.i("SQLiteDebug","read")
    //            val reversalList = reversal.getReversal()
    //
    //        }

        }

        fun accessDatabase(){

            appDatabase = AppDatabase.getAppDatabase(this)
            reversalDAO = appDatabase?.reversalDao()
            saleDAO = appDatabase?.saleDao()

        }


        @Subscribe(threadMode = ThreadMode.MAIN)
        public fun onMessageEvent(event: MessageEvent){

            reverseFlag = false
            output1?.setText("Response Message: " + event.message)
            Log.i("log_tag", "Response Message:" + event.message)
            responseCode = codeUnpack(event.message,39)
            output2?.setText("response code: " + responseCode)
            Log.i("log_tag", "response code:"+ responseCode)

            if(responseCode == "3030"){

                if(stuckReverse == true){

                    Log.i("log_tag", "Reversal Approve.")
                    reversalApprove()
                    var reStan = codeUnpack(reReversal.toString(),11)
                    var reversalApprove = SaleEntity(null,reReversal.toString(), reStan!!.toInt())

                    Thread{

                        accessDatabase()
                        saleDAO?.insertSale(reversalApprove)
                        readSale = saleDAO?.getSale()?.isoMsg
                        readStan = saleDAO?.getSale()?.STAN
                        Log.i("log_tag","saveReversalApprove :  " + readSale)
                        Log.i("log_tag","saveSTAN : " + readStan)

                    }.start()

                    stuckReverse = false

                    stan = stan?.plus(1)
                    saleMsg = salePacket(stan.toString())
                    Log.i("log_tag", "Current stan: " + stan)

                    reversalMsg = reversalPacket(stan.toString())
                    var reverseTrans = ReversalEntity(null,reversalMsg.toString())

                    reverseFlag = true
                    Log.i("log_tag", "send sale packet")
                    sendPacket(saleMsg)
                    Log.i("log_tag", "sale: " + saleMsg.toString())
                    Log.i("log_tag", "reverseFlag:  " + reverseFlag)
                    //                Log.i("log_tag", "else" + reverseFlag)

                    runOnUiThread {
                        output2?.setText("Sale packet:  " + saleMsg.toString())
                    }

                    //reverse สำหรับ transaction ปัจจุบัน
                    Thread{

                        accessDatabase()
                        reversalDAO?.insertReversal(reverseTrans)
                        reReversal = reversalDAO?.getReversal()?.isoMsg
//                        Log.i("log_tag","reReversal:  " + reReversal.toString() )

                    }.start()


                }else{

                    Log.i("log_tag", "Transaction Approve.")
                    transactionApprove()
                    var saleApprove = SaleEntity(null,saleMsg.toString(),stan)

                    Thread{

                        accessDatabase()

                        saleDAO?.insertSale(saleApprove)
                        readSale = saleDAO?.getSale()?.isoMsg
                        readStan = saleDAO?.getSale()?.STAN
                        Log.i("log_tag","saveTransactionApprove :  " + readSale)
                        Log.i("log_tag","saveSTAN : " + readStan)

                    }.start()

                }

            }else{

                if(responseCode == "3934"){

                    errorCode(responseCode,"Seqence error / Duplicate transmission")

                }else{

                    errorCode(responseCode,null)

                }

                Log.i("log_tag", "Error code: " + responseCode)
                var saleApprove = SaleEntity(null,null.toString(),stan)

                Thread{

                    accessDatabase()

                    saleDAO?.insertSale(saleApprove)
                    readSale = saleDAO?.getSale()?.isoMsg
                    readStan = saleDAO?.getSale()?.STAN
                    Log.i("log_tag","saveTransactionNonApprove :  " + readSale)
                    Log.i("log_tag","saveSTAN : " + readStan)


                }.start()

//
            }


            Log.i("log_tag", "reverseFlag:  " + reverseFlag)


        }

        override fun onStart() {
            super.onStart()
            EventBus.getDefault().register(this)
        }

        override fun onStop() {
            super.onStop()
            EventBus.getDefault().unregister(this)
        }

        fun sendPacket(packet: ISOMessage?){
            Thread {
                try {
                    var client = ISOClientBuilder.createSocket(HOST, PORT)
                        .configureBlocking(false)
                        .setReadTimeout(5000)
                        .build()
                    client.connect()

                    var response = bytesArrayToHexString(client.sendMessageSync(packet))
                    EventBus.getDefault().post(MessageEvent(
                        "iso_response",
                        response.toString()
                    ))
//                    Log.i("log_tag", "response : $response")
                    client.disconnect()

                } catch (err: ISOClientException) {
                    Log.e("log_tag", "error1 is ${err.message}")
                    if (err.message.equals("Read Timeout")) {

                        runOnUiThread {
//
                            if(stuckReverse == true){
                                reverselNonApproveTimeout()
                            }else{
                                timeoutAlert()
                            }
                            output2?.setText("Reverse flag: " + reverseFlag)
                            Log.i("log_tag", "reverseFlag:  " + reverseFlag)
                        }
                    }

                } catch(err: ISOException){
                    Log.e("log_tag", "error2 is ${err.message}")
                } catch (err: IOException){
//                    Log.e("log_tag", "error3 is ${err.message}")
                    if (err.message!!.indexOf("ECONNREFUSED") > -1) {
                        Log.e("log_tag", "connection fail.")

                        runOnUiThread {
//
                            if(stuckReverse == true){
                                reverselNonApproveConnectLoss()
                            }else{
                                connectionFailAlert()
                            }
                            output2?.setText("Reverse flag: " + reverseFlag)
                            Log.i("log_tag", "reverseFlag:  " + reverseFlag)
                        }
                    }
                }
            }.start()
        }

        fun reBuildISOPacket(packet: String): ISOMessage? {
            val isoMessage: ISOMessage = ISOMessageBuilder.Unpacker()
                .setMessage(packet)
                .build()
            return isoMessage
        }

        fun codeUnpack(response: String,field: Int): String? {
            val isoMessageUnpacket: ISOMessage = ISOMessageBuilder.Unpacker()
                .setMessage(response)
                .build()
            val responseCode: String? = bytesArrayToHexString(isoMessageUnpacket.getField(field))
            return responseCode
        }



        @Throws(ISOException::class, ISOClientException::class, IOException::class)
        fun salePacket(STAN: String): ISOMessage? {
            return ISOMessageBuilder.Packer(VERSION.V1987)
                    .financial()
                    .setLeftPadding(0x00.toByte())
                    .mti(MESSAGE_FUNCTION.Request, MESSAGE_ORIGIN.Acquirer)
                    .processCode("000000")
                    .setField(FIELDS.F2_PAN, "4444444444444444")
                    .setField(FIELDS.F4_AmountTransaction, amount)
                    .setField(FIELDS.F11_STAN, STAN)
                    .setField(FIELDS.F14_ExpirationDate, "2409")
                    .setField(FIELDS.F22_EntryMode, "0010")
                    .setField(FIELDS.F24_NII_FunctionCode, "120")
                    .setField(FIELDS.F25_POS_ConditionCode, "00")
                    .setField(FIELDS.F41_CA_TerminalID,hexStringToByteArray("3232323232323232"))
                    .setField(FIELDS.F42_CA_ID,hexStringToByteArray("323232323232323232323232323232"))
                    .setField(FIELDS.F62_Reserved_Private,hexStringToByteArray("303030343841"))
                    .setHeader("6001208000")
                    .build()

        }

        fun reversalPacket(STAN: String): ISOMessage? {
            return ISOMessageBuilder.Packer(VERSION.V1987)
                .reversal()
                .setLeftPadding(0x00.toByte())
                .mti(MESSAGE_FUNCTION.Request, MESSAGE_ORIGIN.Acquirer)
                .processCode("000000")
                .setField(FIELDS.F2_PAN, "4444444444444444")
                .setField(FIELDS.F4_AmountTransaction, amount)
                .setField(FIELDS.F11_STAN, STAN)
                .setField(FIELDS.F14_ExpirationDate, "2409")
                .setField(FIELDS.F22_EntryMode, "0010")
                .setField(FIELDS.F24_NII_FunctionCode, "120")
                .setField(FIELDS.F25_POS_ConditionCode, "00")
                .setField(FIELDS.F41_CA_TerminalID,hexStringToByteArray("3232323232323232"))
                .setField(FIELDS.F42_CA_ID,hexStringToByteArray("323232323232323232323232323232"))
                .setField(FIELDS.F62_Reserved_Private,hexStringToByteArray("303030343841"))
                .setHeader("6001208000")
                .build()
        }


        private fun testNetwork(): ISOMessage {
            return ISOMessageBuilder.Packer(VERSION.V1987)
                    .networkManagement()
                    .setLeftPadding(0x00.toByte())
                    .mti(MESSAGE_FUNCTION.Request, MESSAGE_ORIGIN.Acquirer)
                    .processCode("990000")
                    .setField(FIELDS.F24_NII_FunctionCode,"120")
                    .setField(FIELDS.F41_CA_TerminalID,hexStringToByteArray("3232323232323232"))
                    .setField(FIELDS.F42_CA_ID,hexStringToByteArray("323232323232323232323232323232"))
                    .setField(FIELDS.F62_Reserved_Private,hexStringToByteArray("303030343841"))
                    .setHeader("6001208000")
                    .build()
        }

        fun timeoutAlert() {
            val builder = AlertDialog.Builder(this)

            builder.setTitle("Transaction failed!!")
            builder.setMessage("Timeout! have no response message.This transaction must be cancelled.")
           //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

            builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                Toast.makeText(applicationContext,
                    android.R.string.ok, Toast.LENGTH_LONG).show()
            }
            val dialog = builder.create()
            dialog.show()
        }

    fun connectionFailAlert() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Transaction failed!!")
        builder.setMessage("Connection failed! Please check your network.This transaction must be cancelled.")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            Toast.makeText(applicationContext,
                android.R.string.ok, Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun reverselNonApproveTimeout() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Cancel failed!!.Stuck in reverse.")
        builder.setMessage("Failed to cancel previous transaction.Timeout! have no response message")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            Toast.makeText(applicationContext,
                android.R.string.ok, Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun reverselNonApproveConnectLoss() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Cancel failed!!.Stuck in reverse.")
        builder.setMessage("Failed to cancel previous transaction.Connection failed! have no response message")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            Toast.makeText(applicationContext,
                android.R.string.ok, Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }


    fun sendReversal() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Transaction failed!!.Stuck in reverse.")
        builder.setMessage("Automatic canceling previous transaction.")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            Toast.makeText(applicationContext,
                android.R.string.ok, Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun reversalApprove() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Cenceling Success.")
        builder.setMessage("Successfully canceled the transaction.")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            Toast.makeText(applicationContext,
                android.R.string.ok, Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun transactionApprove() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Transaction State.")
        builder.setMessage("Transaction Approve.")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(getString(R.string.ok),DialogInterface.OnClickListener{ dialog, which ->
            Toast.makeText(applicationContext,android.R.string.ok, Toast.LENGTH_LONG).show()
            startActivity(Intent(this,TestActivity::class.java))
        })

        var dialog = builder.create()
        dialog.show()
    }



    fun errorCode(code: String?,msg: String?) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Transaction Error.")
        builder.setMessage("Error code: " + code +",  ${msg}")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            Toast.makeText(applicationContext,
                android.R.string.ok, Toast.LENGTH_LONG).show()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun hexToAscii(hexStr: String): String? {
        val output = java.lang.StringBuilder("")
        var i = 0
        while (i < hexStr.length) {
            val str = hexStr.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }


    private fun hexStringToByteArray(s: String): ByteArray? {
            val b = ByteArray(s.length / 2)
            for (i in b.indices) {
                val index = i * 2
                val v = s.substring(index, index + 2).toInt(16)
                b[i] = v.toByte()
            }
            return b
        }

        private fun bytesArrayToHexString(b1: ByteArray): String? {
            val strBuilder = StringBuilder()
            for (`val` in b1) {
                strBuilder.append(String.format("%02x", `val` and 0xff.toByte()))
            }
            return strBuilder.toString()
        }
}



