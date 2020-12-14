## Projeto

Este projeto tem como objetivo processar arquivos de um diretório de entrada de forma contínua e apresentar um relatório de saída em formato de arquivo em outro diretório.
<br/> Foi utilizazado para o desenvolvimento Scala como linguagem e Flink como framework. 
Ambos foram adotados, pois o desafio deixa de livre escolha. 
A escolha da linguagem Scala está relacionado a deixa o código menos verboso e também permite trabalhar melhor com o paradigma funcional.
O framework flink trabalha também com JAVA, mas daria muitas linhas de códigos a mais se tivesse optado por este caminho.    

## Relatórios emitidos na saída
Para cada arquivo adicionado no diretório de entrada, será coletada as métricas abaixo e será adicionada a informação em um arquivo de saída com o mesmo nome do arquivo de entrada que está sendo lido.

##### 1. Quantidade de clientes no arquivo de entrada
##### 2. Quantidade de vendedores no arquivo de entrada
##### 3. Id da venda mais cara
Se houver mais de uma venda que empate neste critério, apenas um dos ids serão apresentados, pois o requisito se encontra no singular.
##### 4. O pior vendedor
Esse relatório levará em consideração também os vendedores que não tiveram nenhuma venda, ou seja, vendedores que não possuem nenhuma linha com o identificador 003.<br/>
Se houver mais de um vendedor sem nenhuma venda, ou seja, que empatem no critério de pior vendedor, apenas um deles será mostrado na saída, devido o requisito estar no singular e não no plural. 

## Pré-requisitos
1. JDK 1.8

## VM options
O projeto fornece algumas VM Options que podem ser utilizadas.

1. `-Dfile.config=application-custom.conf` - Permite definir um arquivo de conf customizado no momento que estamos desenvolvendo, para não mexer no que é utilizado pelo projeto no ambiente produtivo.
2. `-Ddevelopment.mode=[true|false]` - Permite acionar alguns recursos no projeto que só serão habilitados em no momento de desenvolvimento.

Olhar o arquivo `ilegra.infrastructure.ApplicationVMOption` para mais informações.

## Estrutura

##### Main Class
`ilegra.ApplicationJob`

##### Infrastructure
Contém as classes que configuram o projeto.

1. `ilegra.infrastructure.ApplicationConfiguration` - é um conjunto de classes que constroem os objetos que parametrizam o projeto.
2. `ilegra.infrastructure.FileConfiguration` - constrói as configurações do source do Flow do flink.
3. `ilegra.infrastructure.flow` - Neste pacote teremos a contrução da pipeline do flink, assim como a chamada para inicializar as TaskManager do flink.

## Testes
Foram realizados os testes unitários das partes mais críticas do projeto, onde ocorre a lógica para gerar os relatórios.

##### Gerador de arquivo de teste

Foi desenvolvido um gerador de arquivo no formato solicitado no desafio, para que fosse fácil testar a aplicação.<br/>
Olhar arquivo: `ilegra.GenerateFileMock`