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
2. Scala 2.11

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

## Docker
O projeto está preparado para rodar o flink pelo docker, para isso são necessários ajustar algumas configurações:
1. Ajustar os volumes para os diretórios que conterá os arquivos de entrada e saída.<br/> 
Para não ter que editar o `docker-compose.yml`, basta apenas abrir o arquivo `start-docker.sh` e setar cada um dos diretórios.
2. Após executar o comando `./gradlew jar` para gerar o artefato
3. Pra executar o docker, utilizar o comando `sudo start-docker.sh`

##### docker-compose.yml
Neste arquivo é possível perceber que foram mapeados 4 volumes para os taskmanager:<br/>
`- ${IN_DIR}:/tmp/in` : diretório onde os arquivos ficaram sendo monitorados;<br/>
`- ${OUT_DIR}:/tmp/out` : diretório onde os relatórios serão reportados<br/>
`- ./build/libs:/opt/flink/usrlib` : diretório onde se encontra o artefato .jar<br/>
`- ./docker/init:/tmp/init` : diretório que contém o `docker-entrypoint.sh` customizado que irá dar permissão de escrita e leitura para os diretórios de entrada e saída de arquivos dentro do container após sua inicialização.

## Mode Desenvolvimento
1. IDE utilizada: IntelliJ IDEA Community
2. Configurar no arquivo `resources/application.conf` os caminhos para os diretórios de entrada e saída de arquivos, propriedades:<br/>
- `filePath`: colocar o caminho para o diretório que receberá os arquivos de entrada
- `filePathOut`:  colocar o caminho para o diretório que receberá os arquivos de saída (relatórios)
