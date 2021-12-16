class SplitVariable {

   void method (int p, int j) {
      int x<caret>;

      if (p == 1) {
         x= 2 + p;
         System.out.println(x);
      } else  if (p==2){
         if (j ==0) {
            x= 3;
            System.out.println(x);
         }
      } else {
         x = 2;
         x++;
         System.out.println(x);
      }
   }
}
