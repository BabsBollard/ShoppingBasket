package controllers;

import controllers.security.CheckIfCustomer;
import controllers.security.Secured;
import models.products.Product;
import models.shopping.*;
import models.users.*;

import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.*;
import views.html.*;

// Import models
// Import security controllers
//Authenticate User
@Security.Authenticated(Secured.class)
//Authenticate user(check if user is a customer)
@With(CheckIfCustomer.class)


public class ShoppingCtrl extends Controller {
    
    // Get a user - if logged in email will be set in the session
	private Customer getCurrentUser() {
		return (Customer)User.getLoggedIn(session().get("email"));
	}
//this connects the customer to the basket
    @Transactional
    public Result addToBasket(Long id) {
        //find the product
        Product p = Product.find.byId(id);
        //get basket for logged in customer
        Customer customer = (Customer)User.getLoggedIn(session().get("email"));
        //check if item in basket
        if(customer.getBasket() == null){
            //if no basket, create one
            customer.setBasket(new Basket());
            customer.getBasket().setCustomer(customer);
            customer.update();
        }
        //add product to the basket and save
        customer.getBasket().addProduct(p);
        customer.update();
        //show the basket
        return ok(basket.render(customer));
    }
    //Add an item to the basket
    @Transactional
    public Result addOne(Long itemId){
        //get the order item
        OrderItem item = OrderItem.find.byId(itemId);
        //increment quantity
        item.increaseQty();
        //save/persist
        item.update();
        //show updated basket
        return redirect(routes.ShoppingCtrl.showBasket());
    }
    //remove an item from the basket
    @Transactional
    public Result removeOne(Long itemId){
        //get the order item
        OrderItem item = OrderItem.find.byId(itemId);
        //get user
        Customer c = getCurrentUser();
        //call basket remove item method
        c.getBasket().removeItem(item);
        c.getBasket().update();
        //back to basket
        return ok(basket.render(c));
    }
    @Transactional
    public Result showBasket(){
        return ok(basket.render(getCurrentUser()));
    }
    

    



    // Empty Basket
    @Transactional
    public Result emptyBasket() {
        
        Customer c = getCurrentUser();
        c.getBasket().removeAllItems();
        c.getBasket().update();
        
        return ok(basket.render(c));
    }

        //place order
    @Transactional
    public Result placeOrder(){
        Customer c = getCurrentUser();
        //create an order instance
        ShopOrder order = new ShopOrder();
        //associate order with customer
        order.setCustomer(c);
        //copy basket to order
        order.setItems(c.getBasket().getBasketItems());
        //save the order now to generate a new id for this order
        order.save();
        //move items from basket to order
        for(OrderItem i: order.getItems()) {
            //associate with order
            i.setOrder(order);
            //remove from basket
            i.setBasket(null);
            c.getBasket().setBasketItems(null);
            c.getBasket().update();
        }
            //show order confirmed view
            return ok(orderConfirmed.render(c, order));

    }
    
    // View an individual order
    @Transactional
    public Result viewOrder(long id) {
        ShopOrder order = ShopOrder.find.byId(id);
        return ok(orderConfirmed.render(getCurrentUser(), order));
    }

}