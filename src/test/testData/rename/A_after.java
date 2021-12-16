class SplitVariable {

   void method (int p, int j) {

      if (p == 1) {
         int x= 2 + p;
         System.out.println(x);
      } else  if (p==2){
         if (j ==0) {
            int x= 3;
            System.out.println(x);
         }
      } else {
         int x = 2;
         x++;
         System.out.println(x);
      }
   }
}
