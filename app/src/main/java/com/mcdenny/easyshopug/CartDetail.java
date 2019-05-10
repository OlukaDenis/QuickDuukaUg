package com.mcdenny.easyshopug;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mcdenny.easyshopug.Common.Common;
import com.mcdenny.easyshopug.Model.Cart;
import com.mcdenny.easyshopug.Utils.Cons;
import com.mcdenny.easyshopug.Utils.Util;
import com.mcdenny.easyshopug.ViewHolder.CartDetailViewHolder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.mcdenny.easyshopug.ShippingAddress.PICK_ADDRESS_DETAILS;

public class CartDetail extends AppCompatActivity {
    TextView totalPrice, noCart;
    ImageView noCartImg;
    LinearLayout checkoutLayout;
    Button placeOrder;
    public int total = 0;//cart total
    List<Cart> list = new ArrayList<>();
    HashMap<String, List<Cart>> cartItemMap;

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference orders;
    DatabaseReference cart;
    public String cartItemKey;

    public String currentUserEmail;
    public String currentUserPhone;
    public String currentUserName;

    private static final int PICK_ADDRESS = 23;
    public static final String CUSTOMER_ADDRESS = "CUSTOMER_ADDRESS";
    public static final String CUSTOMER_CITY = "CUSTOMER_CITY";
    public static final String CUSTOMER_REGION = "CUSTOMER_REGION";
    public static final String TOTAL_AMOUNT = "TOTAL_AMOUNT";
    public static final String CURRENT_USER_PHONE = "CURRENT_USER_PHONE";
    public static final String CURRENT_USER_NAME = "CURRENT_USER_NAME";
    public static final String ITEMS_LIST = "ITEMS_LIST";

    private String mCustomerArea;
    private String mCustomerPlace;

    private ProgressDialog progressDialog;
    public android.app.AlertDialog waitingDialog;

    FirebaseRecyclerAdapter<Cart, CartDetailViewHolder> cartAdapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the fonts
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/MontserratRegular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_cart);
        setTitle("Your Cart");
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);

        progressDialog = new ProgressDialog(this);
        waitingDialog = new SpotsDialog(this);

        cartItemMap = new HashMap<>();
        totalPrice = findViewById(R.id.total);
        placeOrder = findViewById(R.id.place_order_btn);
        noCart = findViewById(R.id.noCartItem);
        noCartImg = findViewById(R.id.noCartWaarning);
        checkoutLayout = findViewById(R.id.checkOutLayout);

        placeOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //startActivity(new Intent(CartDetail.this, ShippingAddress.class));
                //  viewAlertDialog();
                pickAddress();
            }
        });

        currentUserEmail = Util.cleanEmailKey(Common.current_user_email);
        currentUserName = Common.current_user_name;
        currentUserPhone = Common.current_user_phone;

        //initialise firebase
        database = FirebaseDatabase.getInstance();
        orders = database.getReference("Requests");
        cart = database.getReference("Cart").child(Common.clean_current_user_email);



        recyclerView = findViewById(R.id.cartList);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //loadCartOrders();
        loadCartDetails();
    }

    private void loadCartDetails() {
       waitingDialog.show();
        //progressDialog.show();
        cartAdapter = new FirebaseRecyclerAdapter<Cart, CartDetailViewHolder>(
                Cart.class,
                R.layout.cart_layout,
                CartDetailViewHolder.class,
                cart
        ) {
            @Override
            protected void populateViewHolder(CartDetailViewHolder viewHolder, Cart model, final int position) {

                list.add(model);
                //sending the cart items to the common activity
                Common.Current_cart_list = list;

                final String price = model.getPrice();
                final String qty = model.getQuantity();
                viewHolder.cart_item_name.setText(model.getName());
                viewHolder.cart_item_price.setText(Cons.Vals.CURRENCY + Util.formatNumber(price));
                viewHolder.cart_number_button.setNumber(qty);
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(viewHolder.cart_item_image);

                final int itemPrice = Integer.parseInt(price);
                final int itemQty = Integer.parseInt(qty);

                total += itemPrice * itemQty;
                totalPrice.setText(Cons.Vals.CURRENCY + Util.formatNumber(String.valueOf(total)));
                Common.cart_item_total = total; // send the total to the common activity

                hideEmptyCart();

                if(list.isEmpty()){
                    waitingDialog.dismiss();
                }
                final String refKey = cartAdapter.getRef(position).getKey();
                //final int mItemPrice = Integer.parseInt(cart.child(refKey).child("price").toString());
                //final int mItemQty = Integer.parseInt(cart.child(refKey).child("quantity").toString());
                //final int removeAmount = mItemPrice * mItemQty;
                //total -= removeAmount;
                viewHolder.remove_cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteCart(refKey);
                        loadCartDetails();
                        totalPrice.setText(Cons.Vals.CURRENCY + Util.formatNumber(String.valueOf(total)));
                        cartAdapter.notifyDataSetChanged();
                        cartAdapter.notifyItemRemoved(position);
                    }
                });
                waitingDialog.dismiss();
            }

        };

        cartAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(cartAdapter);
    }

    private  void deleteCart(String key){
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Deleting item....");
        progressDialog.show();

        cart.child(key).removeValue();
        progressDialog.dismiss();
    }

    private void pickAddress() {

        Intent intent = new Intent(this, ShippingAddress.class);
        intent.putExtra(PICK_ADDRESS_DETAILS, true);
        startActivityForResult(intent, PICK_ADDRESS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_ADDRESS && resultCode == RESULT_OK) {

            mCustomerArea = data.getStringExtra("customerArea");
            mCustomerPlace = data.getStringExtra("customerPlace");
            viewAlertDialog();
        } else {
            Toast.makeText(this, "No Customer address found!", Toast.LENGTH_SHORT).show();
        }
    }

    //method that is called when place order button is clicked
    private void viewAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CartDetail.this);
        alertDialog.setTitle("Confirm your Address");
        alertDialog.setMessage("Name: " + currentUserName +
                "\nContact: " + currentUserPhone +
                "\nAddress: " + mCustomerArea + "," + mCustomerPlace );


        alertDialog.setPositiveButton("PROCEED TO DELIVERY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final android.app.AlertDialog waitingDialog = new SpotsDialog(CartDetail.this);
                waitingDialog.show();
                Intent intent = new Intent(CartDetail.this, DeliveryMethodActivity.class);
                /*
                intent.putExtra(CartDetail.ITEMS_LIST, (Serializable) list);
                intent.putExtra(CartDetail.TOTAL_AMOUNT, total);
                */
                startActivity(intent);
                finish();
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }



    //To hide empty cart
    private void hideEmptyCart(){
        //default view when the cart is empty
        checkoutLayout.setVisibility(View.VISIBLE);
        noCart.setVisibility(View.GONE);
        noCartImg.setVisibility(View.GONE);
    }

    //To unhide empty cart
    private void unhideEmptyCart(){
        //default view when the cart is empty
        checkoutLayout.setVisibility(View.INVISIBLE);
        noCart.setVisibility(View.VISIBLE);
        noCartImg.setVisibility(View.VISIBLE);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_cart, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Connect with me on facebook via: www.facebook.com/denislucaz");
            startActivity(Intent.createChooser(shareIntent, "Send Invite Via"));
            return true;
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(CartDetail.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}



