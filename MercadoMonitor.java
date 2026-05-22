// Nunca cambia la declaracion del package!
package cc.mercado;

import java.util.Map;

import es.upm.babel.cclib.Monitor;
/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class MercadoMonitor implements Mercado {
  // TODO: añadir atributos para representar el estado del recurso y
  // la gestión de la concurrencia (monitor y conditions)

  //Creamos el monitor y las condiciones necesarias para la sincronización
  private final Monitor mutex;
  private final Monitor.Cond alertaBajo;
  private final Monitor.Cond alertaAlto;

  private int cn; //Contador para asignar identificadores únicos a cada oferta
  private int mx; //Precio máximo registrado en el mercado
  private int mi; //Precio mínimo registrado en el mercado

  private final Map<Integer, Oferta> ventas; //Estructura para almacenar las ofertas de venta
  private final Map<Integer, Oferta> compras; //Estructura para almacenar las ofertas de compra

  public MercadoMonitor() {
    // TODO: inicializar estado, monitor y conditions
    this.mutex = new Monitor(); //Inicializamos el monitor
    this.alertaBajo = this.mutex.newCond(); //Incializamos la condición para alertas de precio bajo
    this.alertaAlto = this.mutex.newCond(); //Inicializamos la condición para alertas de precio alto
    this.cn = 0; //Inicializamos el contador de ofertas
    this.mx = Integer.MIN_VALUE; //Inicializamos el precio máximo al valor mínimo posible (- infinito)
    this.mi = Integer.MAX_VALUE; //Inicializamos el precio mínimo al valor máximo posible (infinito)
    this.ventas = new HashTableMap<>(); //Inicializamos la estructura para almacenar las ofertas de venta
    this.compras = new HashTableMap<>(); //Inicializamos la estructura para almacenar las ofertas
     
  }

  public int MatchV(int minPrecio, int tks){
    int id = -1; //Identificador de la oferta de compra compatible
    int precioCompra = -1; //Precio de la oferta de compra compatible
    for(int identificador : compras.keys()){
      Oferta oferta = compras.get(identificador); //Obtenemos la oferta de compra actual
      if(oferta.precio >= minPrecio && oferta.tks > 0 && oferta.precioOferta == 0){ //Si la oferta de compra cumple con el precio mínimo y la cantidad de tokens requerida por la oferta de venta
        if(oferta.precio > precioCompra){ //Si el precio de la oferta de compra es mayor que el precio de la oferta de compra compatible actual
          precioCompra = oferta.precio; //Actualizamos el precio de la oferta de compra compatible
          id = identificador; //Actualizamos el identificador de la oferta de compra compatible
        }
        if(oferta.precio == precioCompra && identificador < id){ //Si el precio de la oferta de compra es igual al precio de la oferta de compra compatible actual
            id = identificador;
        } //Guardamos el identificador de la oferta de compra compatible
      }
    }
    return id;
  }

  public int MatchC(int maxPrecio, int tks){
    int id = -1; //Identificador de la oferta de compra compatible
    int precioCompra = Integer.MAX_VALUE; //Precio de la oferta de compra compatible
    for(int identificador : ventas.keys()){
      Oferta oferta = ventas.get(identificador); //Obtenemos la oferta de venta actual
      if(oferta.precio <= maxPrecio && oferta.tks > 0 && oferta.precioOferta == 0){ //Si la oferta de venta cumple con el precio máximo y la cantidad de tokens requerida por la oferta de compra
        if(oferta.precio < precioCompra){ //Si el precio de la oferta de venta es mayor que el precio de la oferta de compra compatible actual
          precioCompra = oferta.precio; //Actualizamos el precio de la oferta de compra compatible
          id = identificador; //Actualizamos el identificador de la oferta de compra compatible
        }
        if(oferta.precio == precioCompra && identificador < id){ //Si el precio de la oferta de compra es igual al precio de la oferta de compra compatible actual
            id = identificador;
        } //Guardamos el identificador de la oferta de compra compatible
      }
    }
    return id;
  }

  public int venta(int minPrecio, int tks) {
    // TODO: implementar venta
    mutex.enter();
    int id = cn; //Asignamos un identificador único a la oferta de venta
    cn++; //Incrementamos el contador de ofertas para la próxima oferta
    int compraCompatible = MatchV(minPrecio, tks);
      if(tks == 0 || compraCompatible == -1){
        Oferta ofertaVenta = new Oferta(minPrecio, tks, 0); //Creamos una nueva oferta de venta con el precio mínimo y la cantidad de tokens requerida
        ventas.put(id, ofertaVenta); //Agregamos la oferta de venta a la estructura
      } 
      //Si el precio de la oferta de compra es mayor o igual al precio minimo de la oferta de venta:
      if(tks > 0 && compraCompatible != -1){ //Si la oferta de compra cumple con el precio mínimo y la cantidad de tokens requerida por la oferta de venta
        Oferta oferta = compras.get(compraCompatible); //Obtenemos la oferta de compra compatible
        oferta.precioOferta = (minPrecio + oferta.precio)/2; //Actualizamos el precio de la oferta de compra para que coincida con el precio mínimo de la oferta de venta  
        Oferta ofertaVenta = new Oferta(minPrecio, tks, oferta.precioOferta); //Creamos una nueva oferta de venta con el precio mínimo y la cantidad de tokens requerida
        Oferta ofertaCompra = new Oferta(oferta.precio, oferta.tks, oferta.precioOferta); //Creamos una nueva oferta de compra con el precio de la oferta de compra y la cantidad de tokens requerida
        ventas.put(id, ofertaVenta); //Agregamos la oferta de venta a la estructura
        compras.put(compraCompatible, ofertaCompra); //Actualizamos la oferta de compra en la estructura
        mx = Math.max(mx, oferta.precioOferta); //Actualizamos el precio máximo registrado en el mercado
        mi = Math.min(mi, oferta.precioOferta); //Actualizamos el precio mínimo registrado en el mercado
      }
    mutex.leave();
    return id; //Devolvemos el identificador de la oferta de venta
  }

  public int compra(int maxPrecio, int tks) {
    // TODO: implementar compra
    mutex.enter();
    int id = cn; //Asignamos un identificador único a la oferta de compra
    cn++; //Incrementamos el contador de ofertas para la próxima oferta
    int ventaCompatible = MatchC(maxPrecio, tks);
      if(tks == 0 || ventaCompatible == -1){
        Oferta ofertaCompra = new Oferta(maxPrecio, tks, 0); //Creamos una nueva oferta de compra con el precio máximo y la cantidad de tokens requerida
        compras.put(id, ofertaCompra); //Agregamos la oferta de compra a la estructura
      } 
      //Si el precio de la oferta de venta es menor o igual al precio máximo de la oferta de compra:
      if(tks > 0 && ventaCompatible != -1){ //Si la oferta de venta cumple con el precio máximo y la cantidad de tokens requerida por la oferta de compra
        Oferta oferta = ventas.get(ventaCompatible); //Obtenemos la oferta de venta compatible
        oferta.precioOferta = (maxPrecio + oferta.precio)/2; //Actualizamos el precio de la oferta de venta para que coincida con el precio máximo de la oferta de compra  
        Oferta ofertaCompra = new Oferta(maxPrecio, tks, oferta.precioOferta); //Creamos una nueva oferta de compra con el precio máximo y la cantidad de tokens requerida
        Oferta ofertaVenta = new Oferta(oferta.precio, oferta.tks, oferta.precioOferta); //Creamos una nueva oferta de venta con el precio de la oferta de venta y la cantidad de tokens requerida
        compras.put(id, ofertaCompra); //Agregamos la oferta de compra a la estructura
        ventas.put(ventaCompatible, ofertaVenta); //Actualizamos la oferta de venta en la estructura
        mx = Math.max(mx, oferta.precioOferta); //Actualizamos el precio máximo registrado en el mercado
        mi = Math.min(mi, oferta.precioOferta); //Actualizamos el precio mínimo registrado en el mercado
      }
    mutex.leave();
    return id;
  }

  public int resultadoOferta(int id) {
    // TODO: implementar resultadoOferta
    mutex.enter();
    if()
    return -1;
  }

  public void alertaPrecioBajo(int limite) {
    // TODO: implementar alertaPrecioBajo
  }
  
  public void alertaPrecioAlto(int limite) {
    // TODO: implementar alertaPrecioAlto
  }

  public void tick() {
    // TODO: implementar tick
  }
}

//Creamos 
private class Oferta{
  int precio;
  int tks;
  int precioOferta;

  public Oferta(int precio, int tks, int precioOferta){
    this.precio = precio;
    this.tks = tks;
    this.precioOferta = precioOferta;
  } 
}