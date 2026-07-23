# Orion Infra AWS

Infraestrutura como código com **AWS CDK em Java** para provisionar uma aplicação simples e funcional na AWS com foco em organização por stacks e uso consciente do **AWS Free Tier**.

A arquitetura provisiona VPC, ECS Cluster, ECS Service, Application Load Balancer, ECR, RDS MySQL, CloudWatch e Auto Scaling, com separação por classes para facilitar manutenção, evolução e entendimento da solução.

## Arquitetura

A solução foi organizada com stacks específicas para cada responsabilidade da infraestrutura: `OrionVpcStack`, `OrionClusterStack`, `OrionRdsStack` e `OrionServiceStack`, além da classe principal `InfraApp` para orquestração do deploy.

Fluxo resumido da arquitetura:

- A imagem da aplicação é construída com Docker e enviada ao Amazon ECR.
- O ECS Fargate consome essa imagem para subir a aplicação em containers sem gerenciar servidores.
- O Application Load Balancer recebe o tráfego e distribui para o service no ECS.
- O RDS MySQL armazena os dados da aplicação em sub-redes privadas/isoladas.
- O CloudWatch centraliza logs e métricas do ambiente para observabilidade.
- O Auto Scaling ajusta a quantidade de tasks conforme a necessidade configurada.

![Imagem](https://drive.google.com/uc?export=view&id=1Ji9dwy94pvr0rvQ8Ycz9U9bgpMlvB1AX)
![Imagem](https://drive.google.com/uc?export=view&id=1Y378brAXuZ7wWMNLh4GbWILIAjKx0lUQ)

## Serviços utilizados

| Serviço | Finalidade                                          |
|---|-----------------------------------------------------|
| Amazon VPC | Isolar a rede da aplicação, sub-redes e segurança básica |
| Amazon ECS (Fargate) | Executar os containers da aplicação sem gerenciar EC2 |
| Amazon ECR | Armazenar a imagem Docker da aplicação              |
| Application Load Balancer | Receber tráfego HTTP e encaminhar para o serviço ECS |
| Amazon RDS MySQL | Banco de dados relacional da aplicação              |
| Amazon CloudWatch | Logs, eventos e métricas da infraestrutura e da aplicação |
| Application Auto Scaling | Escalar o número de tasks do ECS conforme uso de CPU/memória |
| AWS CloudFormation | Provisionamento dos recursos a partir dos templates gerados pelo CDK |

## Estrutura do projeto

A base do projeto foi criada com `cdk init app --language java`, que gera uma aplicação CDK compatível com Maven e Java, permitindo organizar a infraestrutura em código com classes e stacks reutilizáveis.

```text
src/main/java/com/myorg/
├── OrionInfraApp.java
├── OrionVpcStack.java
├── OrionClusterStack.java
├── OrionRdsStack.java
└── OrionServiceStack.java
```

### Responsabilidade de cada classe

- `InfraApp.java`: ponto de entrada da aplicação CDK e definição da ordem entre stacks.
- `VpcStack.java`: criação da VPC, sub-redes públicas e privadas/isoladas, com atenção ao Free Tier.
- `ClusterStack.java`: criação do cluster ECS.
- `RdsStack.java`: criação do banco RDS MySQL.
- `ServiceStack.java`: criação do ECS Service, ALB, logs, integração com ECR e regras de scaling.

## Pré-requisitos

Antes de executar o projeto, é necessário ter as ferramentas abaixo instaladas e configuradas corretamente, porque o CDK em Java depende de JDK, Maven, AWS CLI e do CDK CLI.

- AWS CLI configurada com `aws configure`.
- Node.js instalado para execução do CDK CLI.
- AWS CDK CLI instalado globalmente.
- Java 17 ou superior.
- Apache Maven.
- Docker instalado e em execução.
- Conta AWS com permissões para VPC, ECS, ECR, RDS, IAM, CloudFormation e CloudWatch.

## Tabela de comandos

Abaixo está uma tabela resumida com os principais comandos utilizados durante a criação e o deploy da infraestrutura.

| Comando | O que faz                                                                   |
|---|-----------------------------------------------------------------------------|
| `cdk init app --language java` | Cria a estrutura inicial de um projeto AWS CDK usando Java                  |
| `mvn clean package` | Compila o projeto Java e gera os artefatos com Maven                        |
| `cdk bootstrap aws://SEU_ACCOUNT_ID/us-east-1` | Prepara a conta/região para receber deploys do CDK                          |
| `cdk synth` | Gera o template CloudFormation a partir do código CDK                       |
| `cdk diff` | Mostra as diferenças entre a infraestrutura atual e a nova versão a ser implantada |
| `cdk deploy --all` | Faz o deploy de todas as stacks do projeto                                  |
| `cdk deploy Vpc` | Faz o deploy apenas da stack de VPC                                         |
| `cdk deploy Cluster` | Faz o deploy apenas da stack do cluster ECS                                 |
| `cdk deploy Rds --parameters Rds:dbPassword=SuaSenha` | Faz o deploy da stack do banco passando a senha como parâmetro              |
| `cdk deploy Service` | Faz o deploy apenas da stack de serviço da aplicação                        |
| `cdk destroy --all` | Remove todas as stacks criadas pelo projeto para evitar custos desnecessários |
| `aws configure` | Configura credenciais, região padrão e formato de saída da AWS CLI          |
| `aws ecr create-repository --repository-name minha-app-image --region us-east-1` | Cria um repositório no Amazon ECR para armazenar a imagem Docker            |
| `aws ecr get-login-password --region us-east-1` `docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com` | Autentica o Docker no registro do Amazon ECR|
| `docker build -t minha-app-image .` | Gera a imagem Docker local da aplicação.                                    |
| `docker tag minha-app-image:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/minha-app-image:latest` | Associa uma tag da imagem local ao endereço do repositório ECR              |
| `docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/minha-app-image:latest` | Envia a imagem Docker para o Amazon ECR                                     |
| `aws cloudformation describe-stacks` | Lista ou consulta stacks do CloudFormation para apoio em diagnóstico        |
| `aws ecs describe-services --cluster NOME_CLUSTER --services NOME_SERVICE` | Consulta detalhes de um service no ECS para troubleshooting                 |

## Passo a passo de execução

## 1. Criar o projeto CDK

```bash
mkdir orion-infra-aws
cd orion-infra-aws
cdk init app --language java
```

Esse comando inicializa a aplicação CDK em Java e gera a base do projeto com Maven e arquivos padrão do CDK.

## 2. Ajustar a estrutura das stacks

Após a criação do projeto, a estrutura pode ser reorganizada para separar a infraestrutura em classes específicas, melhorando a legibilidade e a manutenção do código.

## 3. Criar o repositório no ECR

```bash
aws ecr create-repository --repository-name minha-app-image --region us-east-1
```

O ECR armazena a imagem da aplicação que será utilizada pelo ECS durante o deploy.

## 4. Build e push da imagem Docker

```bash
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS \
    --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

docker build -t minha-app-image .
docker tag minha-app-image:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/minha-app-image:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/minha-app-image:latest
```

A AWS documenta esse fluxo como o caminho padrão para autenticar, taguear e publicar imagens no Amazon ECR.

## 5. Bootstrap da conta AWS

```bash
cdk bootstrap aws://SEU_ACCOUNT_ID/us-east-1
```

O bootstrap prepara recursos necessários para que o CDK consiga publicar ativos e executar deploys na conta/região escolhida.

## 6. Build do projeto Java

```bash
mvn clean package
```

Esse passo garante que o código Java da infraestrutura está compilando corretamente antes da geração dos templates.

## 7. Sintetizar e validar o template

```bash
cdk synth
cdk diff
```

`cdk synth` converte o código em template CloudFormation, enquanto `cdk diff` ajuda a revisar mudanças antes do deploy.

## 8. Fazer o deploy das stacks

```bash
cdk deploy --all --parameters Rds:dbPassword=SuaSenhaSegura123
```

Também é possível subir cada stack separadamente para facilitar testes e diagnóstico:

```bash
cdk deploy Vpc
cdk deploy Cluster
cdk deploy Rds --parameters Rds:dbPassword=SuaSenhaSegura123
cdk deploy Service
```

O deploy individual pode ser útil quando se deseja validar a infraestrutura por partes e identificar mais facilmente em qual stack um problema ocorreu.

## 9. Destruir o ambiente quando necessário

```bash
cdk destroy --all
```

Esse comando remove os recursos provisionados e é especialmente importante em ambientes de estudo para evitar cobranças desnecessárias.

## Configurações importantes do projeto

### VPC

A VPC foi projetada para conter sub-redes públicas e privadas/isoladas, com foco em organização de rede e separação entre entrada pública e banco de dados.

### ECS / Fargate

O ECS Fargate foi utilizado para evitar gerenciamento manual de instâncias EC2, mantendo a infraestrutura mais simples para estudo e operação.

### RDS MySQL

O RDS MySQL foi configurado como banco relacional da aplicação, idealmente em sub-redes isoladas para melhorar segurança.

### CloudWatch

Os logs da aplicação e eventos do ECS podem ser consultados no CloudWatch para diagnóstico de falhas e observabilidade do ambiente.

### Auto Scaling

O Auto Scaling do serviço permite ajustar o número de tasks conforme o consumo configurado de CPU e memória, deixando o ambiente mais resiliente.

## Boas práticas adotadas

- Separação da infraestrutura em stacks menores e com responsabilidade única.
- Uso de IaC com versionamento em código.
- Organização da solução para facilitar estudo e manutenção.
- Uso do ECR para versionamento de imagens.
- Uso do CloudWatch para observabilidade.
- Planejamento com atenção ao AWS Free Tier.

## Troubleshooting

### CloudFormation preso em `CREATE_IN_PROGRESS`

Se uma stack permanecer muito tempo em criação, vale verificar os eventos no CloudFormation e depois analisar ECS Service, Tasks, Target Group e CloudWatch Logs, pois o CloudFormation pode ficar aguardando a estabilização do ECS Service.

### ECS Service não estabiliza

Possíveis causas comuns:

- imagem não encontrada no ECR;
- porta do container incorreta;
- health check do ALB incompatível com a aplicação;
- falha de inicialização da aplicação;
- erro de conexão com o banco.

### Task sobe e cai rapidamente

Nesse cenário, os logs no CloudWatch geralmente mostram a causa raiz com mais clareza, como erro de ambiente, falha no datasource ou problema de credenciais.